package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityViewAllExpensesBinding

/**
 * Activity that serves as a central hub for expense management functionalities.
 * It provides buttons to navigate to different expense-related activities
 * like adding, removing, or viewing expenses, and managing categories/income.
 */
class ViewAllExpensesActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "ViewAllExpensesActivity"

    // Declare the binding variable for accessing views in the layout
    private lateinit var binding: ActivityViewAllExpensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Inflate the layout using view binding and set it as the content view
        binding = ActivityViewAllExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        // Enable edge-to-edge support for immersive UI
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Apply window insets to account for system UI (status bar, nav bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Initialize the bottom navigation menu
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")

        // Handle click on "Add Expense" button
        binding.AddExpenseBtn.setOnClickListener {
            Log.d(TAG, "AddExpenseBtn clicked. Navigating to AddExpenseActivity.") // Log button click
            // Navigate to AddExpenseActivity
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Handle click on "Remove Expense" button
        binding.RemoveExpenseBtn.setOnClickListener {
            Log.d(TAG, "RemoveExpenseBtn clicked. Navigating to RemoveExpenseActivity.") // Log button click
            // Navigate to RemoveExpenseActivity
            startActivity(Intent(this, RemoveExpenseActivity::class.java))
        }

        // Handle click on "Category/Income" button
        binding.CatIncomeBtn.setOnClickListener {
            Log.d(TAG, "CatIncomeBtn clicked. Navigating to CategoryIncomeActivity.") // Log button click
            // Navigate to CategoryIncomeActivity
            startActivity(Intent(this, CategoryIncomeActivity::class.java))
        }

        // Handle click on "View Expense" button
        binding.ViewExpenseBtn.setOnClickListener {
            Log.d(TAG, "ViewExpenseBtn clicked. Navigating to ViewExpenses activity.") // Log button click
            // Navigate to ViewExpenses activity
            startActivity(Intent(this, ViewExpenses::class.java))
        }
        Log.d(TAG, "onCreate: All button click listeners set.")
    }

    /**
     * Sets up the bottom navigation view and defines the actions to be taken
     * when different menu items are selected (e.g., navigating to fragments).
     */
    private fun setupBottomNav() {
        Log.d(TAG, "setupBottomNav: Setting up bottom navigation.") // Log setup initiation
        // Find the BottomNavigationView using its ID from the layout
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    Log.d(TAG, "BottomNav: Logout selected.") // Log menu item selection
                    // Replace current fragment with LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Menu -> {
                    Log.d(TAG, "BottomNav: Menu selected.") // Log menu item selection
                    // Replace current fragment with Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.BudgetingGuides -> {
                    Log.d(TAG, "BottomNav: BudgetingGuides selected.") // Log menu item selection
                    // Replace current fragment with BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Awards -> {
                    Log.d(TAG, "BottomNav: Awards selected.") // Log menu item selection
                    // Replace current fragment with AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                else -> {
                    Log.d(TAG, "BottomNav: Unrecognized item selected (ID: ${item.itemId}).") // Log unrecognized item
                    false // Unrecognized item
                }
            }
        }
        Log.d(TAG, "setupBottomNav: Bottom navigation listener set.") // Log completion of setup
    }
}