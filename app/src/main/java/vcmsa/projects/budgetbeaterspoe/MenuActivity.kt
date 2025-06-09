package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import androidx.activity.enableEdgeToEdge // Enables edge-to-edge layout to provide a full-screen experience
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityMenuBinding

/**
 * The main menu activity of the application.
 * This activity serves as a hub for navigating to different features
 * and includes a bottom navigation bar for common actions.
 */
class MenuActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "MenuActivity"

    // View binding instance for accessing layout elements
    private lateinit var binding: ActivityMenuBinding // Used to reference the layout's views

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Inflate the layout using ViewBinding
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set the root view for this activity
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        // Allows the layout to use the entire screen area, including the status bar and navigation bar
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Check if a user is currently logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(TAG, "onCreate: No user logged in. Redirecting to LoginActivity.") // Log redirection
            // No user logged in, redirect to Login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Finish this activity so the user cannot go back to it without logging in
            return // Stop further execution in this onCreate method
        } else {
            Log.d(TAG, "onCreate: User logged in: ${currentUser.email} (UID: ${currentUser.uid})") // Log logged-in user
        }

        // Set the padding of the main layout to adjust for system bars (status bar and navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // Get system bars insets
            // Apply the insets as padding to the view to prevent content from overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return insets to continue handling window insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Set up click listeners for each button to launch corresponding activities
        binding.viewPieChartBtn.setOnClickListener {
            Log.d(TAG, "viewPieChartBtn clicked: Launching PieChartActivity.") // Log button click
            startActivity(Intent(this, PieChartActivity::class.java)) // Launch PieChartActivity
        }
        binding.viewAllExpensesBtn.setOnClickListener {
            Log.d(TAG, "viewAllExpensesBtn clicked: Launching ViewAllExpensesActivity.") // Log button click
            startActivity(Intent(this, ViewAllExpensesActivity::class.java)) // Launch ViewAllExpensesActivity
        }
        binding.viewDailySpendingBtn.setOnClickListener {
            Log.d(TAG, "viewDailySpendingBtn clicked: Launching ViewAllSpendingActivity.") // Log button click
            // Option to launch either DailySpendingActivity or ViewAllSpendingActivity
            // startActivity(Intent(this, DailySpendingActivity::class.java))
            startActivity(Intent(this, ViewAllSpendingActivity::class.java)) // Launch ViewAllSpendingActivity
        }

        binding.viewProgressDashboardBtn.setOnClickListener {
            Log.d(TAG, "viewProgressDashboardBtn clicked: Launching ProgressDashboardActivity.") // Log button click
            startActivity(Intent(this, ProgressDashboardActivity::class.java)) // Launch ProgressDashboardActivity
        }
        binding.sharedBudgetingBtn.setOnClickListener {
            Log.d(TAG, "sharedBudgetingBtn clicked: Launching SharedBudgetingActivity.") // Log button click
            startActivity(Intent(this, SharedBudgetingActivity::class.java)) // Launch SharedBudgetingActivity
        }

        binding.categoriesBtn.setOnClickListener {
            Log.d(TAG, "categoriesBtn clicked: Launching CategoriesActivity.") // Log button click
            startActivity(Intent(this, CategoriesActivity::class.java)) // Launch CategoriesActivity
        }
        Log.d(TAG, "onCreate: All feature button listeners set.")

        // Set up the BottomNavigationView to switch between different fragments based on selected item
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // If 'Logout' item is selected, replace the fragment container with the LogoutFragment
                R.id.Logout -> {
                    Log.d(TAG, "BottomNav: Logout item selected. Replacing fragment_container with LogoutFragment.") // Log selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment()) // Replace with LogoutFragment
                        .commit() // Commit the transaction
                    true // Indicate that the event was handled
                }

                // If 'Menu' item is selected, replace the fragment container with the Menu_NavFragment
                R.id.Menu -> {
                    Log.d(TAG, "BottomNav: Menu item selected. Replacing fragment_container with Menu_NavFragment.") // Log selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment()) // Replace with Menu_NavFragment
                        .commit() // Commit the transaction
                    true // Indicate that the event was handled
                }

                // If 'BudgetingGuides' item is selected, replace the fragment container with the BudgetingGuidesFragment
                R.id.BudgetingGuides -> {
                    Log.d(TAG, "BottomNav: BudgetingGuides item selected. Replacing fragment_container with BudgetingGuidesFragment.") // Log selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment()) // Replace with BudgetingGuidesFragment
                        .commit() // Commit the transaction
                    true // Indicate that the event was handled
                }

                // If 'Awards' item is selected, replace the fragment container with the AwardsFragment
                R.id.Awards -> {
                    Log.d(TAG, "BottomNav: Awards item selected. Replacing fragment_container with AwardsFragment.") // Log selection
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment()) // Replace with AwardsFragment
                        .commit() // Commit the transaction
                    true // Indicate that the event was handled
                }

                // Default case if an unrecognized item is selected (should not happen with defined menu items)
                else -> {
                    Log.w(TAG, "BottomNav: Unrecognized item selected with ID: ${item.itemId}.") // Log warning for unrecognized item
                    false // Indicate that the event was not handled
                }
            }
        }
        Log.d(TAG, "onCreate: BottomNavigationView listeners set.")
    }
}