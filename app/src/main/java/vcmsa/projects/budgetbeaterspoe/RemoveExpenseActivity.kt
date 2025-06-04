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
        loadExpenses()

        setupBottomNav()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(emptyList()) { expense ->
            showDeleteConfirmationDialog(expense)
        }
        recyclerView.adapter = adapter
    }

    private fun loadExpenses() {
        val uid = currentUserId ?: return

        // Query Firestore collection "expenses" filtered by current user
        firestore.collection("expenses")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val expenses = querySnapshot.documents.map { doc ->
                    ExpenseEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        userId = doc.getString("userId") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                }
                adapter = ExpenseAdapter(expenses) { expense ->
                    showDeleteConfirmationDialog(expense)
                }
                recyclerView.adapter = adapter
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
                        "Category ID: ${expense.category}"
            )
            .setPositiveButton("Yes") { _, _ ->
                deleteExpense(expense)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteExpense(expense: ExpenseEntity) {
        firestore.collection("expenses")
            .document(expense.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                loadExpenses() // Refresh list
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
