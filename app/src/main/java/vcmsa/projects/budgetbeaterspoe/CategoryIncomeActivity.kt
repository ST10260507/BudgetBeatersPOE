package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

// Activity to display and handle category income related operations
class CategoryIncomeActivity : AppCompatActivity() {

    // Declare views for user inputs and output
    private lateinit var categorySpinner: Spinner  // Spinner to select category
    private lateinit var fromDateInput: EditText  // Input field for selecting the "from" date
    private lateinit var toDateInput: EditText  // Input field for selecting the "to" date
    private lateinit var submitBtn: Button  // Submit button for triggering the calculation
    private lateinit var totalTextView: TextView  // TextView to display total amount spent

    private lateinit var db: AppDatabase  // Database instance for accessing data

    // Lifecycle method to set up the activity
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_income) // Set the activity layout

        // Initialize views
        categorySpinner = findViewById(R.id.categorySpinner)
        fromDateInput = findViewById(R.id.FromDateInput)
        toDateInput = findViewById(R.id.ToDateInput)
        submitBtn = findViewById(R.id.submitBtn)
        totalTextView = findViewById(R.id.totalTextView)

        // Initialize the database instance
        db = AppDatabase.getDatabase(this)

        // Load the categories into the spinner
        loadCategories()

        // Set date pickers for "from" and "to" date inputs
        fromDateInput.setOnClickListener { showDatePicker(fromDateInput) }
        toDateInput.setOnClickListener { showDatePicker(toDateInput) }

        // Set click listener for the submit button
        submitBtn.setOnClickListener {
            handleSubmission()
        }

        // Set up bottom navigation view
        setupBottomNav()
    }

    // Method to load all available categories into the spinner
    private fun loadCategories() {
        lifecycleScope.launch {
            // Fetch categories from the database in a background thread
            val categories = withContext(Dispatchers.IO) {
                db.expenseDao().getAllCategories()
            }

            // Set up the spinner adapter with sorted categories
            val adapter = ArrayAdapter(
                this@CategoryIncomeActivity,
                android.R.layout.simple_spinner_item,
                categories.distinct().sorted()
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter  // Attach the adapter to the spinner
        }
    }

    // Method to show a date picker dialog and set the selected date in the input field
    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            // Format and set the selected date in the input field
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            editText.setText(formattedDate)
        }

        // Show the date picker dialog with the current date as the default
        DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Method to handle the submission of the category income query
    private fun handleSubmission() {
        // Get the selected category and dates from the input fields
        val selectedCategory = categorySpinner.selectedItem?.toString() ?: return
        val fromDate = fromDateInput.text.toString()
        val toDate = toDateInput.text.toString()

        // Check if both dates are provided
        if (fromDate.isBlank() || toDate.isBlank()) {
            // Show a toast message if dates are missing
            Toast.makeText(this, "Please select both dates.", Toast.LENGTH_SHORT).show()
            return
        }

        // Perform the calculation on a background thread
        lifecycleScope.launch {
            val totalSpent = withContext(Dispatchers.IO) {
                // Fetch the total spent amount for the selected category and date range from the database
                db.expenseDao().getTotalSpentForCategoryInRange(selectedCategory, fromDate, toDate)
            }

            // Display the result in the TextView
            val display = totalSpent?.let { "Total Spent: R%.2f".format(it) } ?: "No expenses found for this period."
            totalTextView.text = display
        }
    }

    // Method to set up the bottom navigation view and handle navigation actions
    private fun setupBottomNav() {
        // Set an item selected listener for bottom navigation
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    // Replace the current fragment with the LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    // Replace the current fragment with the Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    // Replace the current fragment with the BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    // Replace the current fragment with the AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> false // Return false if no recognized item is selected
            }
        }
    }
}
