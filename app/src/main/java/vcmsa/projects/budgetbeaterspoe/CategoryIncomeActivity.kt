package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import java.util.*
import java.text.SimpleDateFormat // Import SimpleDateFormat for date formatting if needed elsewhere

// Activity to display and handle category income related operations (likely should be category *expense* or *spending*)
class CategoryIncomeActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "CategoryIncomeActivity"

    // UI elements
    private lateinit var categorySpinner: Spinner
    private lateinit var fromDateInput: EditText
    private lateinit var toDateInput: EditText
    private lateinit var submitBtn: Button
    private lateinit var totalTextView: TextView

    // Firebase instances
    private val db = Firebase.firestore // Firestore database instance
    private val auth = FirebaseAuth.getInstance() // Firebase Authentication instance

    // Suppressing warning related to missing inflated ID (though it's usually good practice to resolve these)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation
        setContentView(R.layout.activity_category_income)

        // Initialize UI components by finding them from the layout
        categorySpinner = findViewById(R.id.categorySpinner)
        fromDateInput = findViewById(R.id.FromDateInput)
        toDateInput = findViewById(R.id.ToDateInput)
        submitBtn = findViewById(R.id.submitBtn)
        totalTextView = findViewById(R.id.totalTextView)
        Log.d(TAG, "onCreate: UI components initialized.")

        // Load categories into the spinner
        loadCategories()
        Log.d(TAG, "onCreate: Initiated loading categories.")

        // Set click listeners for date input fields to show DatePicker
        fromDateInput.setOnClickListener {
            Log.d(TAG, "fromDateInput clicked: Showing date picker.")
            showDatePicker(fromDateInput)
        }
        toDateInput.setOnClickListener {
            Log.d(TAG, "toDateInput clicked: Showing date picker.")
            showDatePicker(toDateInput)
        }

        // Set click listener for the submit button
        submitBtn.setOnClickListener {
            Log.d(TAG, "submitBtn clicked: Handling submission.")
            handleSubmission()
        }

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")
    }

    /**
     * Loads categories from Firestore for the current user and populates the category spinner.
     */
    private fun loadCategories() {
        val userId = auth.currentUser?.uid // Get the current user's ID
        if (userId == null) {
            Log.w(TAG, "loadCategories: User not logged in. Cannot load categories.") // Log warning
            Toast.makeText(this, "User not logged in. Cannot load categories.", Toast.LENGTH_SHORT).show()
            return // Exit if user is not authenticated
        }

        Log.d(TAG, "loadCategories: Fetching categories for user ID: $userId")

        // Query Firestore for user-specific categories
        db.collection("users").document(userId).collection("categories")
            .get()
            .addOnSuccessListener { result ->
                // Extract category names from the documents
                val categoryNames = result.documents.mapNotNull { it.getString("categoryName") }
                    .distinct() // Ensure unique category names
                    .sorted()   // Sort alphabetically
                Log.d(TAG, "loadCategories: Successfully fetched ${categoryNames.size} categories. Names: $categoryNames")

                if (categoryNames.isEmpty()) {
                    Log.d(TAG, "loadCategories: No categories found for this user.")
                    Toast.makeText(this, "No categories found for this user.", Toast.LENGTH_SHORT).show()
                    // Optionally, disable relevant UI elements or show a placeholder in the spinner
                    categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("No Categories"))
                    categorySpinner.isEnabled = false // Disable spinner if no categories
                    submitBtn.isEnabled = false // Disable submit button as there's nothing to select
                } else {
                    // Create and set an ArrayAdapter for the spinner
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item, // Default spinner item layout
                        categoryNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Dropdown view layout
                    categorySpinner.adapter = adapter
                    categorySpinner.isEnabled = true // Ensure spinner is enabled
                    submitBtn.isEnabled = true // Ensure submit button is enabled
                    Log.d(TAG, "loadCategories: Category spinner populated.")
                }
            }
            .addOnFailureListener { e ->
                // Log and show error if fetching categories fails
                Log.e(TAG, "loadCategories: Failed to load categories: ${e.localizedMessage}", e)
                Toast.makeText(this, "Failed to load categories: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                categorySpinner.isEnabled = false // Disable spinner on failure
                submitBtn.isEnabled = false // Disable submit button on failure
            }
    }

    /**
     * Displays a DatePickerDialog and sets the selected date to the provided EditText.
     * @param editText The EditText to which the selected date will be applied.
     */
    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance() // Get current calendar instance
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a DatePickerDialog and define its listener
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
            // Format the selected date to "YYYY-MM-DD"
            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            editText.setText(formattedDate) // Set the formatted date to the EditText
            Log.d(TAG, "showDatePicker: Date selected: $formattedDate for ${if (editText == fromDateInput) "From Date" else "To Date"}")
        }

        // Show the DatePickerDialog
        DatePickerDialog(
            this, dateSetListener,
            year,
            month,
            day
        ).show()
        Log.d(TAG, "showDatePicker: DatePickerDialog shown.")
    }

    /**
     * Handles the submission logic: validates inputs, queries Firestore for expenses
     * within the selected category and date range, and displays the total.
     */
    private fun handleSubmission() {
        val selectedCategory = categorySpinner.selectedItem?.toString()
        val fromDate = fromDateInput.text.toString()
        val toDate = toDateInput.text.toString()

        // Input validation checks
        if (selectedCategory.isNullOrBlank() || selectedCategory == "No Categories") {
            Log.w(TAG, "handleSubmission: No category selected or 'No Categories' placeholder.")
            Toast.makeText(this, "Please select a valid category.", Toast.LENGTH_SHORT).show()
            return
        }

        if (fromDate.isBlank() || toDate.isBlank()) {
            Log.w(TAG, "handleSubmission: From or To date is blank.")
            Toast.makeText(this, "Please select both dates.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "handleSubmission: User not logged in. Cannot retrieve expense data.")
            Toast.makeText(this, "User not logged in. Cannot retrieve data.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "handleSubmission: Querying expenses for category '$selectedCategory' from '$fromDate' to '$toDate' for user: $userId")

        // Query Firestore for expenses
        db.collection("users").document(userId).collection("expenses")
            .whereEqualTo("category", selectedCategory) // Filter by selected category name
            .whereGreaterThanOrEqualTo("date", fromDate) // Filter by date greater than or equal to fromDate
            .whereLessThanOrEqualTo("date", toDate)     // Filter by date less than or equal to toDate
            .get()
            .addOnSuccessListener { result ->
                // Calculate the total amount from queried expenses
                val total = result.documents.sumOf { document ->
                    // Get "amount" field, default to 0.0 if null
                    document.getDouble("amount") ?: 0.0
                }
                Log.d(TAG, "handleSubmission: Successfully retrieved ${result.size()} expenses. Total amount: $total")

                // Format the display text based on the total amount
                val display = if (total > 0)
                    "Total Spent: R%.2f".format(total) // Format to 2 decimal places with 'R' for Rand
                else
                    "No expenses found for this period in this category."

                totalTextView.text = display // Update the TextView
                Log.d(TAG, "handleSubmission: Displaying total: $display")
            }
            .addOnFailureListener { e ->
                // Log and show error if data retrieval fails
                Log.e(TAG, "handleSubmission: Error retrieving expense data: ${e.localizedMessage}", e)
                Toast.makeText(this, "Error retrieving data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Sets up the item selected listener for the bottom navigation view.
     * This method handles fragment transactions based on the selected menu item.
     */
    private fun setupBottomNav() {
        Log.d(TAG, "setupBottomNav: Setting up bottom navigation.")
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    Log.d(TAG, "BottomNav: Logout selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    Log.d(TAG, "BottomNav: Menu selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    Log.d(TAG, "BottomNav: BudgetingGuides selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    Log.d(TAG, "BottomNav: Awards selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> {
                    Log.d(TAG, "BottomNav: Unknown item selected (ID: ${item.itemId}).")
                    false
                }
            }
        }
        Log.d(TAG, "setupBottomNav: Bottom navigation listener set.")
    }
}