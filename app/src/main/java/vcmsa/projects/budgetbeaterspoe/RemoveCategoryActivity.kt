package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityRemoveCategoryBinding

/**
 * Activity for removing user-defined budget categories.
 * It fetches categories specific to the logged-in user, allows selection via a spinner,
 * and deletes the selected category from Firestore.
 */
class RemoveCategoryActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "RemoveCategoryActivity"

    // View binding instance for accessing layout elements
    private lateinit var binding: ActivityRemoveCategoryBinding

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    // FirebaseAuth instance for user authentication
    private val auth = FirebaseAuth.getInstance()

    // Map to store category names to Firestore document IDs for easy lookup
    private var categoryMap = mutableMapOf<String, String>()

    // Stores the Firestore document ID of the currently selected category in the spinner
    private var selectedCategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Inflate the layout using ViewBinding
        binding = ActivityRemoveCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set the root view for this activity
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        // Enable edge-to-edge display for a full-screen experience
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Load categories for the spinner immediately upon creation
        loadCategories()
        // Set up click listeners for the buttons
        setupButtons()

        // Set up window insets listener for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")
    }

    /**
     * Fetches categories from Firestore for the currently logged-in user
     * and populates the spinner with category names.
     */
    private fun loadCategories() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "loadCategories: User not logged in. Cannot load categories.") // Log warning
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            // Optionally, redirect to login or handle unauthenticated state (e.g., clear spinner)
            val emptyAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_white, listOf("Login to view categories"))
            binding.categoriesSpinner.adapter = emptyAdapter
            selectedCategoryId = null
            return
        }
        val userId = currentUser.uid
        Log.d(TAG, "loadCategories: Fetching categories for user ID: $userId")

        // Fetch categories from the 'categories' subcollection under the current user's document
        firestore.collection("users").document(userId).collection("categories")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(TAG, "loadCategories: Categories fetched successfully. Found ${querySnapshot.size()} documents.")
                // Clear the map in case of refresh to avoid duplicate entries
                categoryMap.clear()

                for (doc in querySnapshot.documents) {
                    val name = doc.getString("categoryName") ?: "Unnamed Category" // Default name if not found
                    val id = doc.id // Get the document ID
                    categoryMap[name] = id // Store name-ID pair
                    Log.d(TAG, "loadCategories: Found category: $name (ID: $id)")
                }

                // Get a list of category names for the spinner adapter
                val categoryNames = categoryMap.keys.toList()

                // Check if categories are loaded before setting up spinner
                if (categoryNames.isNotEmpty()) {
                    Log.d(TAG, "loadCategories: Populating spinner with categories.")
                    val adapter = ArrayAdapter(this, R.layout.spinner_item_white, categoryNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.categoriesSpinner.adapter = adapter

                    // Set an item selection listener for the spinner
                    binding.categoriesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View,
                            position: Int,
                            id: Long
                        ) {
                            val selectedName = categoryNames[position]
                            selectedCategoryId = categoryMap[selectedName] // Update selected ID
                            Log.d(TAG, "onItemSelected: Selected category: $selectedName (ID: $selectedCategoryId)")
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            selectedCategoryId = null // Clear selected ID if nothing is selected
                            Log.d(TAG, "onNothingSelected: No category selected.")
                        }
                    }
                } else {
                    // Handle case where no categories are found for the user
                    Log.d(TAG, "loadCategories: No categories found for this user.")
                    Toast.makeText(this, "No categories found for this user.", Toast.LENGTH_SHORT).show()
                    val emptyAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_white, listOf("No categories available"))
                    binding.categoriesSpinner.adapter = emptyAdapter
                    selectedCategoryId = null // Ensure no category is selected
                }
            }
            .addOnFailureListener { e ->
                // Log and show error if fetching categories fails
                Log.e(TAG, "loadCategories: Failed to load categories: ${e.message}", e)
                Toast.makeText(this, "Failed to load categories: ${e.message}", Toast.LENGTH_LONG).show()
                val errorAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_white, listOf("Error loading categories"))
                binding.categoriesSpinner.adapter = errorAdapter
                selectedCategoryId = null
            }
    }

    /**
     * Sets up click listeners for the Confirm Delete and Cancel buttons.
     */
    private fun setupButtons() {
        binding.ConfirmDelBtn.setOnClickListener {
            Log.d(TAG, "ConfirmDelBtn clicked.") // Log button click
            deleteSelectedCategory() // Attempt to delete the selected category
        }

        binding.CancelDelBtn.setOnClickListener {
            Log.d(TAG, "CancelDelBtn clicked. Finishing activity.") // Log button click
            finish() // Close the current activity
        }
    }

    /**
     * Deletes the category identified by `selectedCategoryId` from Firestore.
     */
    private fun deleteSelectedCategory() {
        val id = selectedCategoryId
        if (id == null) {
            Log.w(TAG, "deleteSelectedCategory: No category selected for deletion.") // Log warning
            Toast.makeText(this, "No category selected", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "deleteSelectedCategory: User not logged in. Cannot delete category.") // Log warning
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid
        Log.d(TAG, "deleteSelectedCategory: Attempting to delete category ID: $id for user ID: $userId")

        // Delete the category from the 'categories' subcollection under the current user's document
        firestore.collection("users").document(userId).collection("categories").document(id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "deleteSelectedCategory: Category ID: $id deleted successfully.") // Log success
                Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                loadCategories() // Reload categories to update the spinner after deletion
                finish() // Finish the activity after successful deletion
            }
            .addOnFailureListener { e ->
                // Log and show error if deletion fails
                Log.e(TAG, "deleteSelectedCategory: Failed to delete category ID: $id. Error: ${e.message}", e)
                Toast.makeText(this, "Failed to delete category: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Sets up the item selected listener for the bottom navigation view.
     * This method handles fragment transactions based on the selected menu item.
     */
    private fun setupBottomNav() {
        Log.d(TAG, "setupBottomNav: Setting up bottom navigation.") // Log setup initiation
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    Log.d(TAG, "BottomNav: Logout selected.") // Log menu item selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    Log.d(TAG, "BottomNav: Menu selected.") // Log menu item selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    Log.d(TAG, "BottomNav: BudgetingGuides selected.") // Log menu item selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    Log.d(TAG, "BottomNav: Awards selected.") // Log menu item selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> {
                    Log.d(TAG, "BottomNav: Unknown item selected (ID: ${item.itemId}).") // Log unknown selection
                    false
                }
            }
        }
        Log.d(TAG, "setupBottomNav: Bottom navigation listener set.") // Log completion of setup
    }
}