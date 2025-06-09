package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityAddCategoryBinding

class AddCategoryActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "AddCategoryActivity"

    // ViewBinding instance for accessing layout elements
    private lateinit var binding: ActivityAddCategoryBinding

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    // Firebase Auth instance for getting current user information
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Initialize ViewBinding
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable edge-to-edge display for a more immersive experience
        enableEdgeToEdge()

        // Set up window insets listener to handle system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the main view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")

        // Set click listener for the Save button
        binding.SaveBtn.setOnClickListener {
            Log.d(TAG, "Save button clicked. Attempting to save category.") // Log button click

            // Retrieve input values from EditText fields and trim whitespace
            val categoryName = binding.categoryNameInput.text.toString().trim()
            val description = binding.DescriptionInput.text.toString().trim()
            val maxLimitStr = binding.MaxLimitInput.text.toString().trim()
            val minLimitStr = binding.MinLimitInput.text.toString().trim()

            // Get the current user's ID, default to empty string if user is null (not logged in)
            val userId = auth.currentUser?.uid ?: ""
            Log.d(TAG, "User ID: $userId") // Log the current user ID

            // Validate the user input
            if (validateInput(categoryName, maxLimitStr, minLimitStr)) {
                Log.d(TAG, "Input validation successful.") // Log successful validation

                // Convert maxLimit and minLimit strings to integers
                val maxLimit = maxLimitStr.toInt()
                val minLimit = minLimitStr.toInt()

                // Create a HashMap to store category data for Firestore
                val categoryData = hashMapOf(
                    "categoryName" to categoryName,
                    // Store description as null if empty, otherwise store the value
                    "description" to if (description.isNotEmpty()) description else null,
                    "maxLimit" to maxLimit,
                    "minLimit" to minLimit,
                    "userId" to userId
                )
                Log.d(TAG, "Category data prepared: $categoryData") // Log data to be saved

                // Check if userId is available before attempting to save to Firestore
                if (userId.isNotEmpty()) {
                    // Save the category data to Firestore under the user's categories subcollection
                    firestore.collection("users").document(userId) // Navigate to the user's document
                        .collection("categories") // Access the 'categories' subcollection
                        .add(categoryData) // Add a new document with the generated ID
                        .addOnSuccessListener { documentReference ->
                            // Success listener for Firestore operation
                            Log.d(TAG, "Category saved successfully! Document ID: ${documentReference.id}") // Log success
                            Toast.makeText(
                                this@AddCategoryActivity,
                                "Category saved successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Finish the activity after successful save, returning to the previous screen
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // Failure listener for Firestore operation
                            Log.e(TAG, "Error saving category: ${e.message}", e) // Log error
                            Toast.makeText(
                                this@AddCategoryActivity,
                                "Error saving category: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Log.w(TAG, "User not logged in. Cannot save category.") // Warn if user not logged in
                    Toast.makeText(
                        this@AddCategoryActivity,
                        "User not logged in. Please log in to add a category.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Log.d(TAG, "Input validation failed. Please correct errors.") // Log validation failure
                // Error messages are already set on the input fields by validateInput function
            }
        }
    }

    /**
     * Validates the user input for category name, max limit, and min limit.
     * Sets error messages on respective input fields if validation fails.
     * @param categoryName The name of the category.
     * @param maxLimit The maximum budget limit as a string.
     * @param minLimit The minimum budget limit as a string.
     * @return true if all inputs are valid, false otherwise.
     */
    private fun validateInput(categoryName: String, maxLimit: String, minLimit: String): Boolean {
        Log.d(TAG, "validateInput: Validating category name: '$categoryName', maxLimit: '$maxLimit', minLimit: '$minLimit'")

        var isValid = true // Flag to track overall validation status

        // Validate category name
        if (categoryName.isEmpty()) {
            binding.categoryNameInput.error = "Category name required"
            Log.d(TAG, "Validation failed: Category name is empty.")
            isValid = false
        } else {
            binding.categoryNameInput.error = null // Clear previous error
        }

        // Validate max limit
        if (maxLimit.isEmpty()) {
            binding.MaxLimitInput.error = "Max goal is required"
            Log.d(TAG, "Validation failed: Max limit is empty.")
            isValid = false
        } else if (!maxLimit.all { it.isDigit() }) {
            binding.MaxLimitInput.error = "Only numbers allowed"
            Log.d(TAG, "Validation failed: Max limit contains non-digit characters.")
            isValid = false
        } else {
            binding.MaxLimitInput.error = null // Clear previous error
        }

        // Validate min limit
        if (minLimit.isEmpty()) {
            binding.MinLimitInput.error = "Min goal is required"
            Log.d(TAG, "Validation failed: Min limit is empty.")
            isValid = false
        } else if (!minLimit.all { it.isDigit() }) {
            binding.MinLimitInput.error = "Only numbers allowed"
            Log.d(TAG, "Validation failed: Min limit contains non-digit characters.")
            isValid = false
        } else {
            binding.MinLimitInput.error = null // Clear previous error
        }

        // Additional validation: Check if maxLimit is less than minLimit (if both are valid numbers)
        if (isValid && maxLimit.isNotEmpty() && minLimit.isNotEmpty()) {
            try {
                val max = maxLimit.toInt()
                val min = minLimit.toInt()
                if (max < min) {
                    binding.MaxLimitInput.error = "Max limit cannot be less than Min limit"
                    binding.MinLimitInput.error = "Min limit cannot be greater than Max limit"
                    Log.d(TAG, "Validation failed: Max limit ($max) is less than Min limit ($min).")
                    isValid = false
                } else {
                    // Clear errors if previous check passed
                    binding.MaxLimitInput.error = null
                    binding.MinLimitInput.error = null
                }
            } catch (e: NumberFormatException) {
                // This catch block should ideally not be reached if .all { it.isDigit() } works perfectly,
                // but it's good for robustness.
                Log.e(TAG, "Error parsing min/max limits to Int: ${e.message}", e)
                binding.MaxLimitInput.error = "Invalid number"
                binding.MinLimitInput.error = "Invalid number"
                isValid = false
            }
        }


        Log.d(TAG, "validateInput: Returning isValid = $isValid")
        return isValid
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