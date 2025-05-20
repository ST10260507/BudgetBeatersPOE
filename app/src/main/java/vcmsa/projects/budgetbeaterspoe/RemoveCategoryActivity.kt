package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityRemoveCategoryBinding
import kotlinx.coroutines.launch

// Activity to handle category removal
class RemoveCategoryActivity : AppCompatActivity() {
    // Initialize variables
    private lateinit var binding: ActivityRemoveCategoryBinding
    private var selectedCategoryId: Int? = null // Stores the ID of the selected category
    private var categoryMap = mutableMapOf<String, Int>() // Map to store category names and IDs

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoveCategoryBinding.inflate(layoutInflater) // Bind the layout
        setContentView(binding.root) // Set the activity layout
        enableEdgeToEdge() // Enable edge-to-edge display

        loadCategories() // Load the categories from the database
        setupButtons() // Setup the buttons (Confirm and Cancel)

        // Set listener for window insets to adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        setupBottomNav() // Setup the bottom navigation view
    }

    // Load categories from the database and populate the spinner
    private fun loadCategories() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(applicationContext) // Get database instance
            val categories = database.categoryDao().getAllCategories() // Fetch all categories

            // Create a map from category names to their IDs
            categoryMap = categories.associate { it.categoryName to it.id }.toMutableMap()

            // Get the list of category names
            val categoryNames = categoryMap.keys.toList()

            // Create an adapter for the spinner
            val adapter = ArrayAdapter(this@RemoveCategoryActivity, R.layout.spinner_item_white, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Set dropdown item style
            binding.categoriesSpinner.adapter = adapter // Set the adapter to the spinner

            // Set item selected listener for the spinner
            binding.categoriesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    // Get the ID of the selected category
                    val selectedName = categoryNames[position]
                    selectedCategoryId = categoryMap[selectedName]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedCategoryId = null // Reset selection if nothing is selected
                }
            }
        }
    }

    // Setup the Confirm and Cancel buttons
    private fun setupButtons() {
        binding.ConfirmDelBtn.setOnClickListener {
            deleteSelectedCategory() // Delete the selected category when Confirm button is clicked
        }

        binding.CancelDelBtn.setOnClickListener {
            finish() // Close the activity when Cancel button is clicked
        }
    }

    // Delete the selected category from the database
    private fun deleteSelectedCategory() {
        val id = selectedCategoryId
        if (id == null) {
            Toast.makeText(this, "No category selected", Toast.LENGTH_SHORT).show() // Show error if no category is selected
            return
        }

        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(applicationContext) // Get database instance
            database.categoryDao().deleteCategoriesByIds(listOf(id)) // Delete category from database
            Toast.makeText(this@RemoveCategoryActivity, "Category deleted", Toast.LENGTH_SHORT).show() // Show success message
        }

        finish() // Close the activity after deletion
    }

    // Setup the bottom navigation menu and handle item clicks
    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    // Replace fragment with LogoutFragment when Logout is selected
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    // Replace fragment with Menu_NavFragment when Menu is selected
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    // Replace fragment with BudgetingGuidesFragment when BudgetingGuides is selected
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    // Replace fragment with AwardsFragment when Awards is selected
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> false // Return false if no item is selected
            }
        }
    }
}
