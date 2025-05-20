package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityAddCategoryBinding
import kotlinx.coroutines.launch

// Activity to add a new category to the application
class AddCategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCategoryBinding

    // This method is called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()  // Enables edge-to-edge display for a more immersive UI experience

        // Get a reference to the database
        val db = AppDatabase.getDatabase(applicationContext)

        // Setting padding for system bars like status and navigation bar to avoid overlap with content
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup bottom navigation
        setupBottomNav()

        // Listener for the save button
        binding.SaveBtn.setOnClickListener {
            // Get user input from the fields and trim leading/trailing spaces
            val categoryName = binding.categoryNameInput.text.toString().trim()
            val description = binding.DescriptionInput.text.toString().trim()
            val maxLimitStr = binding.MaxLimitInput.text.toString().trim()
            val minLimitStr = binding.MinLimitInput.text.toString().trim()

            // Validate input fields
            if (validateInput(categoryName, maxLimitStr, minLimitStr)) {
                // Convert max and min limits to integers
                val maxLimit = maxLimitStr.toInt()
                val minLimit = minLimitStr.toInt()

                // Launch a coroutine to save the category in the database
                lifecycleScope.launch {
                    try {
                        val database = AppDatabase.getDatabase(applicationContext)
                        // Insert new category into the database
                        database.categoryDao().insertCategory(
                            CategoryEntity(
                                categoryName = categoryName,
                                description = if (description.isNotEmpty()) description else null,
                                maxLimit = maxLimit,
                                minLimit = minLimit
                            )
                        )

                        // Show a success message and close the activity
                        runOnUiThread {
                            Toast.makeText(
                                this@AddCategoryActivity,
                                "Category saved successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        // Show an error message if saving fails
                        runOnUiThread {
                            Toast.makeText(
                                this@AddCategoryActivity,
                                "Error saving category: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

    }

    // Function to validate the input fields before saving
    private fun validateInput(categoryName: String, maxLimit: String, minLimit: String): Boolean {
        var isValid = true

        // Check if category name is empty
        if (categoryName.isEmpty()) {
            binding.categoryNameInput.error = "Category name required"
            isValid = false
        }

        // Check if max limit is empty or not a number
        if (maxLimit.isEmpty()) {
            binding.MaxLimitInput.error = "Max goal is required"
            isValid = false
        } else if (!maxLimit.all { it.isDigit() }) {
            binding.MaxLimitInput.error = "Only numbers allowed"
            isValid = false
        }

        // Check if min limit is empty or not a number
        if (minLimit.isEmpty()) {
            binding.MinLimitInput.error = "Min goal is required"
            isValid = false
        } else if (!minLimit.all { it.isDigit() }) {
            binding.MinLimitInput.error = "Only numbers allowed"
            isValid = false
        }

        return isValid
    }

    // Function to set up bottom navigation bar
    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            // Handle navigation based on the selected item in the bottom nav
            when (item.itemId) {
                R.id.Logout -> {
                    // Replace fragment with LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    // Replace fragment with Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    // Replace fragment with BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    // Replace fragment with AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
