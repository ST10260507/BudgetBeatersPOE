package vcmsa.projects.budgetbeaterspoe

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date // Import Date class
import java.util.Locale

/**
 * Activity for viewing and filtering expenses.
 * Users can view all their expenses or filter them by a specific date range.
 * Expenses are fetched from Firestore and displayed in a RecyclerView.
 */
class ViewExpenses : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "ViewExpensesActivity"

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FilteredExpenseAdapter // Adapter for displaying expenses in RecyclerView
    private lateinit var fromDateInput: EditText        // EditText for the start date filter
    private lateinit var toDateInput: EditText          // EditText for the end date filter
    private var allExpenses = listOf<ExpenseEntity>() // Stores all expenses for the current user

    private val db = FirebaseFirestore.getInstance() // Firestore database instance
    private val auth = FirebaseAuth.getInstance()    // Firebase authentication instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Enable edge-to-edge support for immersive UI
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_expenses) // Set the layout for this activity
        Log.d(TAG, "onCreate: Layout set to activity_view_expenses.")

        // Apply window insets to account for system UI (status bar, nav bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the view to avoid content overlapping with system bars
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        setupViews()        // Initialize UI components
        setupDatePickers()  // Configure date pickers for date input fields
        setupRecyclerView() // Set up the RecyclerView with its adapter and layout manager
        setupButtons()      // Set up click listeners for filter and view all buttons
        loadAllExpenses()   // Load all user-specific expenses initially
        setupBottomNav()    // Set up the bottom navigation bar
        Log.d(TAG, "onCreate: All initial setups complete.")
    }

    /**
     * Initializes the UI components by finding their respective IDs in the layout.
     */
    private fun setupViews() {
        fromDateInput = findViewById(R.id.FromDateInput)
        toDateInput = findViewById(R.id.ToDateInput)
        recyclerView = findViewById(R.id.expensesRecyclerView)
        Log.d(TAG, "setupViews: UI components initialized.")
    }

    /**
     * Sets up the RecyclerView with a LinearLayoutManager and initializes the adapter.
     */
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FilteredExpenseAdapter(emptyList()) // Initialize adapter with an empty list
        recyclerView.adapter = adapter
        Log.d(TAG, "setupRecyclerView: RecyclerView setup complete.")
    }

    /**
     * Sets up DatePickerDialogs for the "From" and "To" date EditText fields,
     * allowing users to easily select dates.
     */
    private fun setupDatePickers() {
        // Define the date format for parsing and formatting dates
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance() // Get a Calendar instance for date manipulation
        Log.d(TAG, "setupDatePickers: Setting up date pickers.")

        // Lambda function to create and show a DatePickerDialog for a given EditText
        val picker = { editText: EditText ->
            // Try to parse existing text to set initial date in picker
            try {
                val existingDate = editText.text.toString()
                if (existingDate.isNotEmpty()) {
                    calendar.time = dateFormat.parse(existingDate) ?: Calendar.getInstance().time
                }
            } catch (e: Exception) {
                Log.e(TAG, "setupDatePickers: Error parsing existing date for picker: ${e.message}")
            }

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day) // Set the selected date to the calendar
                    // Format the selected date to "yyyy-MM-dd"
                    val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day) // month + 1 because Calendar.MONTH is 0-indexed
                    editText.setText(formattedDate) // Set the formatted date to the EditText
                    Log.d(TAG, "setupDatePickers: Date selected for ${editText.id}: $formattedDate")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show() // Show the DatePickerDialog
        }

        // Attach the DatePickerDialog to the click listeners of the date input fields
        fromDateInput.setOnClickListener { picker(fromDateInput) }
        toDateInput.setOnClickListener { picker(toDateInput) }
        Log.d(TAG, "setupDatePickers: Date picker listeners set.")
    }

    /**
     * Sets up click listeners for the filter ("Submit") and "View All" buttons.
     */
    private fun setupButtons() {
        findViewById<Button>(R.id.submitBtn).setOnClickListener {
            Log.d(TAG, "Submit button clicked.") // Log button click
            val start = fromDateInput.text.toString()
            val end = toDateInput.text.toString()

            // Validate if both date fields are filled
            if (start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "setupButtons: Date range incomplete.")
                return@setOnClickListener
            }

            filterExpenses(start, end) // Call filter function with selected dates
        }

        findViewById<Button>(R.id.ViewAllBtn).setOnClickListener {
            Log.d(TAG, "View All button clicked.") // Log button click
            showAllExpenses() // Display all loaded expenses
        }
        Log.d(TAG, "setupButtons: Button click listeners set.")
    }

    /**
     * Loads all expenses for the current authenticated user from Firestore.
     * The loaded expenses are stored in `allExpenses` and initially displayed.
     */
    private fun loadAllExpenses() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in. Cannot load expenses.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "loadAllExpenses: User not logged in. Aborting expense load.")
            return
        }
        Log.d(TAG, "loadAllExpenses: Loading all expenses for user ID: $userId")

        db.collection("users").document(userId).collection("expenses")
            .get()
            .addOnSuccessListener { result ->
                val tempList = mutableListOf<ExpenseEntity>()
                for (doc in result) {
                    try {
                        val expense = doc.toObject(ExpenseEntity::class.java)
                        // Create a copy to include the document ID
                        tempList.add(expense.copy(id = doc.id))
                        Log.v(TAG, "loadAllExpenses: Loaded expense: ${expense.name} (ID: ${doc.id})")
                    } catch (e: Exception) {
                        Log.e(TAG, "loadAllExpenses: Error converting document ${doc.id} to ExpenseEntity: ${e.message}", e)
                    }
                }
                allExpenses = tempList // Update the master list of all expenses
                adapter.updateExpenses(allExpenses) // Update the RecyclerView adapter
                Log.d(TAG, "loadAllExpenses: Successfully loaded ${allExpenses.size} expenses.")

                if (allExpenses.isEmpty()) {
                    Toast.makeText(this, "No expenses found for this user.", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "loadAllExpenses: No expenses found for the current user.")
                }
            }
            .addOnFailureListener { e ->
                // Log and show error if loading expenses fails
                Toast.makeText(this, "Error loading expenses: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "loadAllExpenses: Failed to load expenses: ${e.localizedMessage}", e)
            }
    }

    /**
     * Filters the `allExpenses` list based on the provided start and end dates.
     * The filtered results are then displayed in the RecyclerView.
     * @param start The start date string in "yyyy-MM-dd" format.
     * @param end The end date string in "yyyy-MM-dd" format.
     */
    private fun filterExpenses(start: String, end: String) {
        Log.d(TAG, "filterExpenses: Filtering expenses from $start to $end.")
        try {
            val inputFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate: Date? = inputFmt.parse(start)
            val endDate: Date? = inputFmt.parse(end)

            // Basic validation for parsed dates
            if (startDate == null || endDate == null) {
                Toast.makeText(this, "Invalid date format. Please use YYYY-MM-DD.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "filterExpenses: Invalid date format: startDate=$start, endDate=$end")
                return
            }

            // Ensure end date is not before start date
            if (startDate.after(endDate)) {
                Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "filterExpenses: End date ($end) is before start date ($start).")
                return
            }

            // Filter expenses that fall within the given date range (inclusive)
            val filtered = allExpenses.filter { exp ->
                try {
                    val expDate = inputFmt.parse(exp.date)
                    expDate != null && expDate.time >= startDate.time && expDate.time <= endDate.time
                } catch (ex: Exception) {
                    // Log parsing errors for individual expense dates but continue filtering
                    Log.e(TAG, "filterExpenses: Error parsing expense date '${exp.date}': ${ex.message}")
                    false // Exclude expenses with unparseable dates from the filtered list
                }
            }

            if (filtered.isEmpty()) {
                Toast.makeText(this, "No expenses found within the selected date range.", Toast.LENGTH_SHORT).show()
                Log.i(TAG, "filterExpenses: No expenses found for the selected range.")
            }
            adapter.updateExpenses(filtered) // Update the RecyclerView with the filtered list
            Log.d(TAG, "filterExpenses: Filtered ${filtered.size} expenses.")
        } catch (e: Exception) {
            // Catch any other exceptions during filtering (e.g., SimpleDateFormat issues)
            Toast.makeText(this, "Error filtering expenses: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "filterExpenses: Unexpected error during filtering: ${e.localizedMessage}", e)
        }
    }

    /**
     * Displays all loaded expenses in the RecyclerView and clears the date filter inputs.
     */
    private fun showAllExpenses() {
        fromDateInput.text.clear() // Clear the "From" date input
        toDateInput.text.clear()   // Clear the "To" date input
        adapter.updateExpenses(allExpenses) // Update adapter to show all expenses
        Log.d(TAG, "showAllExpenses: Displaying all ${allExpenses.size} expenses.")
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