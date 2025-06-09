package vcmsa.projects.budgetbeaterspoe

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityViewAllSpendingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for viewing all spending data, categorized and displayed in a bar chart.
 * Users can filter the spending data by a date range.
 */
class ViewAllSpendingActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "ViewAllSpendingActivity"

    private lateinit var binding: ActivityViewAllSpendingBinding
    private lateinit var barChart: BarChart
    private lateinit var etFromDate: EditText // EditText for the start date filter
    private lateinit var etToDate: EditText    // EditText for the end date filter

    private val firestore = FirebaseFirestore.getInstance() // Firestore database instance
    private val auth = FirebaseAuth.getInstance()        // Firebase authentication instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.")

        // Inflate the layout using view binding and set it as the content view
        binding = ActivityViewAllSpendingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        initializeViews()        // Initialize UI components
        setupDatePickers()       // Configure date pickers for date input fields
        setupChartAppearance()   // Set up the visual appearance of the bar chart
        setupFilterButton()      // Set up the click listener for the filter button
        setupViewCategoryLimitsButton() // <--- NEW: Setup the button for Category Limits
        loadData()               // Load initial spending data
        setupBottomNav()         // Set up the bottom navigation bar
        Log.d(TAG, "onCreate: All initial setups complete.")
    }

    /**
     * Initializes the views from the binding object.
     */
    private fun initializeViews() {
        barChart = binding.barChart
        etFromDate = binding.etFromDate
        etToDate = binding.etToDate
        Log.d(TAG, "initializeViews: UI components initialized.")
    }

    /**
     * Sets up DatePickerDialogs for the "From" and "To" date EditText fields,
     * allowing users to easily select dates.
     */
    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        Log.d(TAG, "setupDatePickers: Setting up date pickers.")

        // Helper function to create and show a DatePickerDialog
        fun createDatePicker(editText: EditText) {
            val calendar = Calendar.getInstance()
            // If the EditText already has text, try to parse it to set the initial date of the picker
            try {
                val existingDate = editText.text.toString()
                if (existingDate.isNotEmpty()) {
                    calendar.time = dateFormat.parse(existingDate) ?: Calendar.getInstance().time
                }
            } catch (e: Exception) {
                Log.e(TAG, "createDatePicker: Error parsing existing date from EditText: ${e.message}")
            }

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    editText.setText(dateFormat.format(calendar.time)) // Set selected date to EditText
                    Log.d(TAG, "createDatePicker: Date selected for ${editText.id}: ${dateFormat.format(calendar.time)}")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Apply click listener and disable keyboard input for etFromDate
        etFromDate.apply {
            setOnClickListener { createDatePicker(this) }
            keyListener = null // Disable direct keyboard input
            Log.d(TAG, "setupDatePickers: etFromDate listener set.")
        }

        // Apply click listener and disable keyboard input for etToDate
        etToDate.apply {
            setOnClickListener { createDatePicker(this) }
            keyListener = null // Disable direct keyboard input
            Log.d(TAG, "setupDatePickers: etToDate listener set.")
        }
    }

    /**
     * Sets up the click listener for the filter button, which reloads the data based on
     * the selected date range.
     */
    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            Log.d(TAG, "Filter button clicked. Reloading data.") // Log button click
            loadData() // Reload data when filter button is clicked
        }
    }

    /**
     * NEW: Sets up the click listener for the "View Category Limits" button.
     */
    private fun setupViewCategoryLimitsButton() {
        binding.btnViewCat.setOnClickListener {
            Log.d(TAG, "View Category Limits button clicked. Navigating to ViewCategoryLimits.")
            val intent = Intent(this, ViewCategoryLimits::class.java)
            startActivity(intent)
            // If you want to finish this activity when navigating away, uncomment the line below:
            // finish()
        }
    }

    /**
     * Configures the basic appearance and interactivity of the BarChart.
     */
    private fun setupChartAppearance() {
        with(barChart) {
            description.isEnabled = false // Disable chart description
            setDrawGridBackground(false) // Do not draw grid background
            setTouchEnabled(true)        // Enable touch interactions
            setPinchZoom(true)           // Enable pinch zoom
            isDragEnabled = true         // Enable dragging
            Log.d(TAG, "setupChartAppearance: Basic chart appearance set.")

            // Configure X-axis properties
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM // Place X-axis labels at the bottom
                granularity = 1f                      // Set granularity to 1 for distinct bars
                setDrawGridLines(false)               // Do not draw X-axis grid lines
                textColor = android.graphics.Color.WHITE // Set X-axis label color to white
                textSize = 10f // Set text size for X-axis labels
                Log.d(TAG, "setupChartAppearance: X-axis configured.")
            }
            // Disable right Y-axis
            axisRight.isEnabled = false
            // Set left Y-axis text color to white
            axisLeft.textColor = android.graphics.Color.WHITE
            axisLeft.textSize = 10f
            Log.d(TAG, "setupChartAppearance: Y-axes configured.")

            // Set legend text color to white
            legend.textColor = android.graphics.Color.WHITE
            legend.textSize = 12f
            Log.d(TAG, "setupChartAppearance: Legend configured.")

            setNoDataText("No spending data available.") // Message when no data is set
            setNoDataTextColor(android.graphics.Color.WHITE) // Color of no data text
        }
    }

    /**
     * Initiates the loading of expense data from Firestore based on the selected date range.
     * Uses a coroutine to perform the asynchronous data fetching.
     */
    private fun loadData() {
        val start = etFromDate.text.toString()
        val end = etToDate.text.toString()
        Log.d(TAG, "loadData: Attempting to load data for range: From='$start', To='$end'")

        lifecycleScope.launch { // Launch a coroutine in the lifecycle scope
            try {
                val expenses = fetchExpensesFromFirestore() // Fetch all expenses

                val filteredExpenses = if (start.isNotEmpty() && end.isNotEmpty()) {
                    Log.d(TAG, "loadData: Filtering expenses by date range.")
                    filterExpensesByDate(expenses, start, end) // Filter if date range is provided
                } else {
                    Log.d(TAG, "loadData: No date range provided. Using all expenses.")
                    expenses // Use all expenses if no date range is specified
                }

                runOnUiThread {
                    if (filteredExpenses.isEmpty()) {
                        showNoDataMessage() // Display message if no filtered expenses
                        Log.d(TAG, "loadData: No filtered expenses found.")
                    } else {
                        displayChartData(filteredExpenses) // Display chart if data exists
                        Log.d(TAG, "loadData: Displaying chart with ${filteredExpenses.size} expenses.")
                    }
                }
            } catch (e: Exception) {
                // Log and show error if data loading fails
                Log.e(TAG, "loadData: Error loading or displaying data: ${e.message}", e)
                showError(e)
            }
        }
    }

    /**
     * Fetches expense data for the current user from Firestore.
     * This is a suspend function as it performs an asynchronous database operation.
     * @return A list of ExpenseEntity objects, or an empty list if no user is logged in or an error occurs.
     */
    private suspend fun fetchExpensesFromFirestore(): List<ExpenseEntity> {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "fetchExpensesFromFirestore: User not logged in. Returning empty list.") // Log warning
            return emptyList()
        }
        Log.d(TAG, "fetchExpensesFromFirestore: Fetching expenses for user ID: $userId")

        return try {
            val snapshot = firestore.collection("users").document(userId).collection("expenses")
                .get()
                .await() // Await the completion of the Firestore get operation

            snapshot.documents.mapNotNull { doc ->
                try {
                    // Map Firestore document data to an ExpenseEntity object
                    // Ensure 'categoryId' is properly handled if it's in your ExpenseEntity and Firestore
                    ExpenseEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        category = doc.getString("category") ?: "Miscellaneous",
                        // categoryId = doc.getString("categoryId") ?: "", // Uncomment if you have categoryId in Firestore
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "1970-01-01", // Default date for robustness
                        userId = doc.getString("userId") ?: userId,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        description = doc.getString("description") ?: ""
                    ).also { Log.v(TAG, "fetchExpensesFromFirestore: Mapped expense: ${it.name} - ${it.amount}") }
                } catch (e: Exception) {
                    Log.e(TAG, "fetchExpensesFromFirestore: Error mapping document ${doc.id}: ${e.message}", e)
                    null // Return null if mapping fails for a document
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchExpensesFromFirestore: Failed to fetch expenses from Firestore: ${e.message}", e)
            emptyList() // Return empty list on any fetching error
        }
    }

    /**
     * Filters a list of expenses based on a given date range (start and end dates).
     * @param expenses The list of all expenses to filter.
     * @param start The start date string in "yyyy-MM-dd" format.
     * @param end The end date string in "yyyy-MM-dd" format.
     * @return A new list containing only the expenses within the specified date range.
     */
    private fun filterExpensesByDate(expenses: List<ExpenseEntity>, start: String, end: String): List<ExpenseEntity> {
        Log.d(TAG, "filterExpensesByDate: Filtering expenses from $start to $end.")
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.parse(start)!!
            val endDate = sdf.parse(end)!!

            expenses.filter { expense ->
                try {
                    val expenseDate = sdf.parse(expense.date)
                    // Check if expenseDate is not null and falls within the start and end dates (inclusive)
                    expenseDate != null && (expenseDate.time >= startDate.time && expenseDate.time <= endDate.time)
                } catch (e: Exception) {
                    Log.e(TAG, "filterExpensesByDate: Error parsing expense date '${expense.date}': ${e.message}")
                    false // Exclude expenses with unparseable dates
                }
            }.also {
                Log.d(TAG, "filterExpensesByDate: Found ${it.size} expenses in range.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "filterExpensesByDate: Error parsing date range: ${e.message}", e)
            Toast.makeText(this, "Invalid date format for filter.", Toast.LENGTH_SHORT).show()
            emptyList() // Return empty list if date parsing fails
        }
    }

    /**
     * Displays the provided list of expenses in the BarChart, grouped by category.
     * @param expenses The list of ExpenseEntity objects to display.
     */
    private fun displayChartData(expenses: List<ExpenseEntity>) {
        Log.d(TAG, "displayChartData: Preparing to display chart for ${expenses.size} expenses.")
        // Group expenses by category and sum their amounts
        val categoryMap = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        // Create BarEntry objects and labels for each category
        categoryMap.entries.sortedByDescending { it.value }.forEachIndexed { i, (category, total) ->
            entries.add(BarEntry(i.toFloat(), total.toFloat()))
            labels.add(category)
            Log.v(TAG, "displayChartData: Category: $category, Total: $total")
        }

        val dataSet = BarDataSet(entries, "Expenses by Category").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList() // Use Material Design colors for bars
            valueTextColor = android.graphics.Color.WHITE  // Set value text color on bars
            valueTextSize = 12f                             // Set value text size on bars
            Log.d(TAG, "displayChartData: BarDataSet created.")
        }

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels) // Set custom labels for X-axis
            labelCount = labels.size                          // Ensure all labels are shown
            setDrawLabels(true) // Ensure labels are drawn
            // Adjust X-axis label rotation if labels are long
            if (labels.size > 4) { // Heuristic: if more than 4 labels, rotate them
                labelRotationAngle = -45f
            } else {
                labelRotationAngle = 0f
            }
        }

        barChart.data = BarData(dataSet) // Set the data to the bar chart
        barChart.setFitBars(true) // Make the bars fit perfectly to the chart
        barChart.animateY(1000)    // Animate the chart along the Y-axis
        barChart.invalidate()      // Refresh the chart to display new data
        Log.d(TAG, "displayChartData: Chart data set and animated.")
    }

    /**
     * Displays a Toast message indicating no data was found and clears the chart.
     */
    private fun showNoDataMessage() {
        Log.i(TAG, "showNoDataMessage: No expenses found for the selected range.")
        Toast.makeText(this, "No expenses found in selected range", Toast.LENGTH_SHORT).show()
        barChart.clear()      // Clear any existing chart data
        barChart.invalidate() // Invalidate the chart to refresh its display
    }

    /**
     * Displays an error message to the user as a Toast.
     * @param e The exception that occurred.
     */
    private fun showError(e: Exception) {
        runOnUiThread { // Ensure UI updates are on the main thread
            Log.e(TAG, "showError: Displaying error toast for: ${e.message}", e) // Log the error
            Toast.makeText(
                this@ViewAllSpendingActivity,
                "Error loading data: ${e.message}",
                Toast.LENGTH_LONG // Use LONG duration for errors
            ).show()
        }
    }

    /**
     * Sets up the bottom navigation view and defines the actions to be taken
     * when different menu items are selected (e.g., navigating to fragments).
     */
    private fun setupBottomNav() {
        Log.d(TAG, "setupBottomNav: Setting up bottom navigation.")
        // Find the BottomNavigationView using its ID from the layout
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    Log.d(TAG, "BottomNav: Logout selected.")
                    // Replace current fragment with LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Menu -> {
                    Log.d(TAG, "BottomNav: Menu selected.")
                    // Replace current fragment with Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.BudgetingGuides -> {
                    Log.d(TAG, "BottomNav: BudgetingGuides selected.")
                    // Replace current fragment with BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Awards -> {
                    Log.d(TAG, "BottomNav: Awards selected.")
                    // Replace current fragment with AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                // No need to add R.id.id_view_category_limits here, as we are using the dedicated button
                else -> {
                    Log.d(TAG, "BottomNav: Unrecognized item selected (ID: ${item.itemId}).")
                    false // Unrecognized item
                }
            }
        }
        Log.d(TAG, "setupBottomNav: Bottom navigation listener set.")
    }
}