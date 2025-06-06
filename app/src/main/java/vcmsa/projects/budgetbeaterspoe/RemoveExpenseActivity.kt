package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Activity responsible for removing expenses
class RemoveExpenseActivity : AppCompatActivity() {

    private lateinit var adapter: ExpenseAdapter
    private lateinit var recyclerView: RecyclerView

    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // User ID (for filtering user's expenses)
    private val currentUserId: String? get() = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_expense)

        recyclerView = findViewById(R.id.expensesRecyclerView)

        findViewById<Button>(R.id.Exit).setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadExpenses() // Load expenses for the current user

        setupBottomNav()
    }

    private fun setupRecyclerView() {
        // Initialize adapter with an empty list. The list will be updated in loadExpenses()
        adapter = ExpenseAdapter(emptyList()) { expense ->
            showDeleteConfirmationDialog(expense)
        }
        recyclerView.adapter = adapter
    }

    private fun loadExpenses() {
        val uid = currentUserId
        if (uid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Query Firestore subcollection "expenses" under the current user's document
        firestore.collection("users").document(uid).collection("expenses")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val expenses = querySnapshot.documents.map { doc ->
                    ExpenseEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "", // Category Name
                        categoryId = doc.getString("categoryId") ?: "", // Category ID
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        userId = doc.getString("userId") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        description = doc.getString("description") // Retrieve description
                    )
                }
                // Update the adapter with the new list of expenses
                adapter.updateExpenses(expenses) // Assuming ExpenseAdapter has an updateExpenses method
                // If not, you'll need to create a new adapter instance
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(expense: ExpenseEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense?")
            .setMessage(
                "Expense: ${expense.name}\n" +
                        "Amount: R${expense.amount}\n" +
                        "Date: ${expense.date}\n" +
                        "Category: ${expense.category}\n" + // Display category name
                        "Category ID: ${expense.categoryId}" // Display category ID
            )
            .setPositiveButton("Yes") { _, _ ->
                deleteExpense(expense)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteExpense(expense: ExpenseEntity) {
        val uid = currentUserId
        if (uid == null) {
            Toast.makeText(this, "User not logged in. Cannot delete expense.", Toast.LENGTH_SHORT).show()
            return
        }

        // Delete from the specific user's subcollection
        firestore.collection("users").document(uid).collection("expenses")
            .document(expense.id) // Use the expense document ID
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                loadExpenses() // Refresh list after deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
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