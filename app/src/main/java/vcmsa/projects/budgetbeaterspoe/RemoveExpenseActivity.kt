package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager // Import for LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity responsible for displaying and allowing the removal of user expenses.
 * It fetches expenses specific to the logged-in user from Firestore,
 * displays them in a RecyclerView, and provides an option to delete them.
 */
class RemoveExpenseActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "RemoveExpenseActivity"

    // Adapter for the RecyclerView to display expenses
    private lateinit var adapter: ExpenseAdapter

    // RecyclerView instance to list expenses
    private lateinit var recyclerView: RecyclerView

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    // FirebaseAuth instance for user authentication
    private val auth = FirebaseAuth.getInstance()

    // Getter for the current user's ID, returns null if no user is logged in
    private val currentUserId: String?
        get() {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Log.w(TAG, "currentUserId: User ID is null, user not logged in.")
            }
            return uid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation
        setContentView(R.layout.activity_remove_expense) // Set the layout for this activity

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.expensesRecyclerView)
        Log.d(TAG, "onCreate: RecyclerView initialized.")

        // Set click listener for the Exit button
        findViewById<Button>(R.id.Exit).setOnClickListener {
            Log.d(TAG, "Exit button clicked. Finishing activity.") // Log button click
            finish() // Close the current activity
        }

        // Set up the RecyclerView with its adapter and layout manager
        setupRecyclerView()
        // Load expenses for the current user into the RecyclerView
        loadExpenses()

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")
    }

    /**
     * Configures the RecyclerView with a LinearLayoutManager and initializes the ExpenseAdapter.
     * The adapter uses a lambda function for item click handling, which triggers a delete confirmation dialog.
     */
    private fun setupRecyclerView() {
        // Initialize adapter with an empty list. The list will be updated in loadExpenses().
        // The lambda function defines the action when an expense item is clicked (e.g., to delete it).
        adapter = ExpenseAdapter(emptyList()) { expense ->
            Log.d(TAG, "setupRecyclerView: Expense item clicked: ${expense.name} (ID: ${expense.id})")
            showDeleteConfirmationDialog(expense)
        }
        recyclerView.adapter = adapter // Set the adapter to the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this) // Set a LinearLayoutManager
        Log.d(TAG, "setupRecyclerView: RecyclerView adapter and layout manager set.")
    }

    /**
     * Fetches expenses from Firestore for the currently logged-in user.
     * It queries the 'expenses' subcollection under the user's document and updates the RecyclerView.
     */
    private fun loadExpenses() {
        val uid = currentUserId
        if (uid == null) {
            Log.w(TAG, "loadExpenses: User not logged in, cannot load expenses.") // Log warning
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            // Clear the adapter if no user is logged in
            adapter.updateExpenses(emptyList())
            return
        }

        Log.d(TAG, "loadExpenses: Fetching expenses for user ID: $uid")

        // Query Firestore subcollection "expenses" under the current user's document
        firestore.collection("users").document(uid).collection("expenses")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(TAG, "loadExpenses: Expenses fetched successfully. Found ${querySnapshot.size()} documents.")
                val expenses = querySnapshot.documents.map { doc ->
                    // Map each Firestore document to an ExpenseEntity object
                    ExpenseEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unnamed Expense",
                        category = doc.getString("category") ?: "Uncategorized", // Category Name
                        categoryId = doc.getString("categoryId") ?: "", // Category ID
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "Unknown Date",
                        userId = doc.getString("userId") ?: uid, // Ensure userId is captured
                        imageUrl = doc.getString("imageUrl") ?: "",
                        description = doc.getString("description") // Retrieve description
                    ).also {
                        Log.d(TAG, "loadExpenses: Mapped expense: ${it.name} - ${it.amount}")
                    }
                }
                // Update the adapter with the new list of expenses
                adapter.updateExpenses(expenses) // Assuming ExpenseAdapter has an updateExpenses method
                Log.d(TAG, "loadExpenses: RecyclerView updated with ${expenses.size} expenses.")
                if (expenses.isEmpty()) {
                    Toast.makeText(this, "No expenses recorded.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Log and show error if fetching expenses fails
                Log.e(TAG, "loadExpenses: Failed to load expenses: ${e.message}", e)
                Toast.makeText(this, "Failed to load expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                adapter.updateExpenses(emptyList()) // Clear list on error
            }
    }

    /**
     * Displays an AlertDialog to confirm the deletion of an expense.
     * @param expense The ExpenseEntity object to be deleted.
     */
    private fun showDeleteConfirmationDialog(expense: ExpenseEntity) {
        Log.d(TAG, "showDeleteConfirmationDialog: Displaying confirmation dialog for expense: ${expense.name}")
        AlertDialog.Builder(this)
            .setTitle("Delete Expense?")
            .setMessage(
                "Are you sure you want to delete this expense?\n\n" +
                        "Expense: ${expense.name}\n" +
                        "Amount: R${String.format("%.2f", expense.amount)}\n" + // Format amount to 2 decimal places
                        "Date: ${expense.date}\n" +
                        "Category: ${expense.category}\n" +
                        "Description: ${expense.description ?: "N/A"}" // Display description, "N/A" if null
            )
            .setPositiveButton("Yes") { dialog, _ ->
                Log.d(TAG, "showDeleteConfirmationDialog: 'Yes' clicked. Deleting expense.")
                deleteExpense(expense) // Proceed with deletion if "Yes" is clicked
                dialog.dismiss() // Dismiss the dialog
            }
            .setNegativeButton("No") { dialog, _ ->
                Log.d(TAG, "showDeleteConfirmationDialog: 'No' clicked. Deletion cancelled.")
                dialog.dismiss() // Dismiss the dialog if "No" is clicked
            }
            .show() // Show the dialog
    }

    /**
     * Deletes the specified expense from the Firestore database.
     * @param expense The ExpenseEntity object representing the expense to delete.
     */
    private fun deleteExpense(expense: ExpenseEntity) {
        val uid = currentUserId
        if (uid == null) {
            Log.w(TAG, "deleteExpense: User not logged in. Cannot delete expense for ID: ${expense.id}.") // Log warning
            Toast.makeText(this, "User not logged in. Cannot delete expense.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "deleteExpense: Attempting to delete expense ID: ${expense.id} for user ID: $uid")

        // Delete the expense document from the specific user's subcollection
        firestore.collection("users").document(uid).collection("expenses")
            .document(expense.id) // Use the expense document ID to target the specific document
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "deleteExpense: Expense ID: ${expense.id} deleted successfully.") // Log success
                Toast.makeText(this, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                loadExpenses() // Refresh the list after successful deletion to reflect changes
            }
            .addOnFailureListener { e ->
                // Log and show error if deletion fails
                Log.e(TAG, "deleteExpense: Failed to delete expense ID: ${expense.id}. Error: ${e.message}", e)
                Toast.makeText(this, "Failed to delete expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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