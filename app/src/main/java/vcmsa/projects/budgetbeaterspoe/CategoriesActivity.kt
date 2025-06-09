package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.Button
import android.widget.Toast // Import for showing toast messages
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Activity to display a list of categories for the current user.
class CategoriesActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "CategoriesActivity"

    // RecyclerView to display the list of categories
    private lateinit var recyclerView: RecyclerView

    // Adapter for the RecyclerView to bind category data to views
    private lateinit var adapter: SimpleCategoryAdapter

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    // Firebase Auth instance for getting current user information
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Set the content view to the categories activity layout
        setContentView(R.layout.activity_categories)

        // Enable edge-to-edge display for a more immersive experience
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Set up the RecyclerView for displaying categories
        setupRecyclerView()
        Log.d(TAG, "onCreate: RecyclerView setup initiated.")

        // Load categories from Firestore
        loadCategoriesFromFirestore()
        Log.d(TAG, "onCreate: Category loading initiated from Firestore.")

        // Set up window insets listener to handle system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the main view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        // Set click listener for the back button
        findViewById<Button>(R.id.backBtn).setOnClickListener {
            Log.d(TAG, "backBtn clicked: Finishing activity.") // Log back button click
            finish() // Close the current activity and return to the previous one
        }

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")
    }

    /**
     * Configures the RecyclerView with a LinearLayoutManager and initializes the adapter.
     */
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.categoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this) // Set a linear layout manager
        adapter = SimpleCategoryAdapter(emptyList()) // Initialize adapter with an empty list
        recyclerView.adapter = adapter // Set the adapter to the RecyclerView
        Log.d(TAG, "setupRecyclerView: RecyclerView initialized and adapter set.")
    }

    /**
     * Loads category data for the current user from Firebase Firestore.
     * Updates the RecyclerView adapter with the fetched categories.
     */
    private fun loadCategoriesFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "loadCategoriesFromFirestore: User not logged in, cannot load categories.") // Warn if user not logged in
            Toast.makeText(this, "User not logged in. Cannot load categories.", Toast.LENGTH_SHORT).show()
            return // Exit if user is not authenticated
        }

        Log.d(TAG, "loadCategoriesFromFirestore: Attempting to load categories for user ID: $userId")

        // Load from user's categories subcollection
        firestore.collection("users").document(userId) // Navigate to the specific user's document
            .collection("categories") // Access the 'categories' subcollection
            .get() // Get all documents in this collection
            .addOnSuccessListener { result ->
                // This block is executed if the Firestore query is successful
                val categories = mutableListOf<CategoryEntity>() // List to hold fetched CategoryEntity objects
                for (document in result) {
                    // Convert each Firestore document to a CategoryEntity object
                    val category = document.toObject(CategoryEntity::class.java)
                    categories.add(category) // Add the category to the list
                    Log.d(TAG, "loadCategoriesFromFirestore: Added category: ${category.categoryName} (ID: ${document.id})")
                }
                // Update the adapter with the new list of categories
                adapter = SimpleCategoryAdapter(categories)
                recyclerView.adapter = adapter
                Log.d(TAG, "loadCategoriesFromFirestore: Successfully loaded ${categories.size} categories and updated RecyclerView.")
                if (categories.isEmpty()) {
                    Toast.makeText(this, "No categories found. Please add some from the menu.", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "loadCategoriesFromFirestore: No categories found for the user.")
                }
            }
            .addOnFailureListener { exception ->
                // This block is executed if the Firestore query fails
                Log.e(TAG, "loadCategoriesFromFirestore: Error loading categories: ${exception.message}", exception) // Log the error
                Toast.makeText(this, "Error loading categories: ${exception.message}", Toast.LENGTH_SHORT).show()
                exception.printStackTrace() // Print stack trace for debugging
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