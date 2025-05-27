package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

// Activity responsible for removing expenses
class RemoveExpenseActivity : AppCompatActivity() {

    // Variables to hold references to the adapter, database, and recycler view
    private lateinit var adapter: ExpenseAdapter
    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_expense) // Set the layout for this activity

        // Initialize the RecyclerView and database
        recyclerView = findViewById(R.id.expensesRecyclerView)
        database = AppDatabase.getDatabase(this)

        // Set the exit button to close the activity when clicked
        findViewById<Button>(R.id.Exit).setOnClickListener {
            finish() // Close the activity
        }

        // Setup the RecyclerView and load expenses
        setupRecyclerView()
        loadExpenses()

        // Setup the bottom navigation menu
        setupBottomNav()
    }

    // Setup the RecyclerView with an adapter and click listener for each expense
    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(emptyList()) { expense ->
            showDeleteConfirmationDialog(expense) // Show confirmation dialog on expense click
        }
        recyclerView.adapter = adapter // Set the adapter for the RecyclerView
    }

    // Load all expenses from the database asynchronously
    private fun loadExpenses() {
        lifecycleScope.launch {
            val expenses = database.expenseDao().getAllExpenses() // Get all expenses from the database
            runOnUiThread {
                // Update the adapter with the fetched expenses
                adapter = ExpenseAdapter(expenses) { expense ->
                    showDeleteConfirmationDialog(expense) // Show confirmation dialog on expense click
                }
                recyclerView.adapter = adapter // Set the updated adapter to the RecyclerView
            }
        }
    }

    // Show a confirmation dialog to confirm expense deletion
    private fun showDeleteConfirmationDialog(expense: ExpenseEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense?") // Set dialog title
            .setMessage(
                // Display expense details in the dialog
                "Expense: ${expense.name}\n" +
                        "Amount: R${expense.amount}\n" +
                        "Date: ${expense.date}\n" +
                        "Category: ${expense.category}"
            )
            .setPositiveButton("Yes") { _, _ ->
                deleteExpense(expense) // Call deleteExpense if user confirms
            }
            .setNegativeButton("No", null) // Close dialog without action if user declines
            .show()
    }

    // Delete the selected expense from the database
    private fun deleteExpense(expense: ExpenseEntity) {
        lifecycleScope.launch {
            database.expenseDao().deleteExpenseById(expense.id) // Delete expense from database
            runOnUiThread {
                loadExpenses() // Reload expenses after deletion
                // Show a Toast message to confirm deletion
                Toast.makeText(
                    this@RemoveExpenseActivity,
                    "Expense deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Setup the bottom navigation menu and handle item clicks
    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    // Replace current fragment with LogoutFragment when "Logout" is clicked
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    // Replace current fragment with Menu_NavFragment when "Menu" is clicked
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    // Replace current fragment with BudgetingGuidesFragment when "Budgeting Guides" is clicked
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    // Replace current fragment with AwardsFragment when "Awards" is clicked
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
