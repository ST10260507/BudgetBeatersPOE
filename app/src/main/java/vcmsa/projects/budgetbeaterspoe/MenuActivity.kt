package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge // Enables edge-to-edge layout to provide a full-screen experience
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding // Used to reference the layout's views

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater) // Inflate the layout using ViewBinding
        setContentView(binding.root) // Set the root view for this activity
        enableEdgeToEdge() // Allows the layout to use the entire screen area, including the status bar and navigation bar

        // Set the padding of the main layout to adjust for system bars (status bar and navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // Get system bars insets
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Apply the insets as padding
            insets // Return insets to continue handling window insets
        }

        // Set up click listeners for each button to launch corresponding activities
        binding.viewPieChartBtn.setOnClickListener {
            startActivity(Intent(this, PieChartActivity::class.java)) // Launch PieChartActivity
        }
        binding.viewAllExpensesBtn.setOnClickListener {
            startActivity(Intent(this, ViewAllExpensesActivity::class.java)) // Launch ViewAllExpensesActivity
        }
        binding.viewDailySpendingBtn.setOnClickListener {
            // Option to launch either DailySpendingActivity or ViewAllSpendingActivity
            // startActivity(Intent(this, DailySpendingActivity::class.java))
            startActivity(Intent(this, ViewAllSpendingActivity::class.java)) // Launch ViewAllSpendingActivity
        }

        binding.viewProgressDashboardBtn.setOnClickListener {
            startActivity(Intent(this, ProgressDashboardActivity::class.java)) // Launch ProgressDashboardActivity
        }
        binding.sharedBudgetingBtn.setOnClickListener {
            startActivity(Intent(this, SharedBudgetingActivity::class.java)) // Launch SharedBudgetingActivity
        }

        binding.categoriesBtn.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java)) // Launch CategoriesActivity
        }

        // Set up the BottomNavigationView to switch between different fragments based on selected item
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // If 'Logout' item is selected, replace the fragment with the LogoutFragment
                R.id.Logout -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment()) // Replace with LogoutFragment
                        .commit()
                    true
                }

                // If 'Menu' item is selected, replace the fragment with the Menu_NavFragment
                R.id.Menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment()) // Replace with Menu_NavFragment
                        .commit()
                    true
                }

                // If 'BudgetingGuides' item is selected, replace the fragment with the BudgetingGuidesFragment
                R.id.BudgetingGuides -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment()) // Replace with BudgetingGuidesFragment
                        .commit()
                    true
                }

                // If 'Awards' item is selected, replace the fragment with the AwardsFragment
                R.id.Awards -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment()) // Replace with AwardsFragment
                        .commit()
                    true
                }

                // Default case if an unrecognized item is selected
                else -> false
            }
        }
    }
}
