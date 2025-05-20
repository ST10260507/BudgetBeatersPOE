package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityViewAllExpensesBinding

class ViewAllExpensesActivity : AppCompatActivity() {

    // Declare the binding variable for accessing views in the layout
    private lateinit var binding: ActivityViewAllExpensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding and set it as the content view
        binding = ActivityViewAllExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable edge-to-edge support for immersive UI
        enableEdgeToEdge()

        // Apply window insets to account for system UI (status bar, nav bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the bottom navigation menu
        setupBottomNav()

        // Handle click on "Add Expense" button
        binding.AddExpenseBtn.setOnClickListener {
            // Navigate to AddExpenseActivity
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Handle click on "Remove Expense" button
        binding.RemoveExpenseBtn.setOnClickListener {
            // Navigate to RemoveExpenseActivity
            startActivity(Intent(this, RemoveExpenseActivity::class.java))
        }

        // Handle click on "Category/Income" button
        binding.CatIncomeBtn.setOnClickListener {
            // Navigate to CategoryIncomeActivity
            startActivity(Intent(this, CategoryIncomeActivity::class.java))
        }

        // Handle click on "View Expense" button
        binding.ViewExpenseBtn.setOnClickListener {
            // Navigate to ViewExpenses activity
            startActivity(Intent(this, ViewExpenses::class.java))
        }
    }

    // Sets up the bottom navigation and handles item selection
    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    // Replace current fragment with LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    // Replace current fragment with Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    // Replace current fragment with BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    // Replace current fragment with AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> false // Unrecognized item
            }
        }
    }
}
