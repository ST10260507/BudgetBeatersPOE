package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log // Import for logging
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * Activity for displaying a progress dashboard, visualized as a pie chart.
 * Allows users to select a month to view spending data and export the chart as an image.
 */
class ProgressDashboardActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "ProgressDashboardActivity"

    // PieChart view instance
    private lateinit var pieChart: PieChart

    // Spinner for selecting the month
    private lateinit var monthSpinner: Spinner

    // Button to export the chart
    private lateinit var exportButton: Button

    // Button to navigate back
    private lateinit var backButton: Button

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    // FirebaseAuth instance for user authentication
    private val auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation
        setContentView(R.layout.activity_progress_dashboard)

        // Adjust layout for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Initialize views
        pieChart = findViewById(R.id.pieChart)
        monthSpinner = findViewById(R.id.monthSpinner)
        exportButton = findViewById(R.id.exportBtn)
        backButton = findViewById(R.id.backBtn)
        Log.d(TAG, "onCreate: Views initialized.")

        // Set up the month selection spinner
        setupMonthSpinner()
        Log.d(TAG, "onCreate: Month spinner setup complete.")

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")

        // Set click listener for the export button
        exportButton.setOnClickListener {
            Log.d(TAG, "exportButton clicked: Exporting chart as image.") // Log button click
            exportChartAsImage()
        }

        // Set click listener for the back button
        backButton.setOnClickListener {
            Log.d(TAG, "backButton clicked: Finishing activity.") // Log button click
            finish() // Close the current activity
        }
    }

    /**
     * Sets up the month selection spinner with month names and handles selection changes.
     */
    private fun setupMonthSpinner() {
        // List of month names
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        // Create an ArrayAdapter using the month list and a custom spinner item layout
        val adapter = ArrayAdapter(this, R.layout.spinner_item_white, months)
        // Set the layout for the dropdown items
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        monthSpinner.adapter = adapter
        Log.d(TAG, "setupMonthSpinner: Adapter set for month spinner.")

        // Set the default selection to the current month
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        monthSpinner.setSelection(currentMonth)
        Log.d(TAG, "setupMonthSpinner: Current month selected in spinner.")

        // Set an item selection listener to update the chart based on the selected month
        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected month (1-12)
                val selectedMonth = position + 1
                // Get the current year
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                Log.d(TAG, "onItemSelected: Month selected: $selectedMonth, updating pie chart data.")
                // Update the pie chart with data for the selected month and year
                setupPieChartDataForMonth(currentYear, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing if nothing is selected
                Log.d(TAG, "onNothingSelected: No month selected.")
            }
        }
    }

    /**
     * Fetches and sets up the pie chart data for the selected month.
     * @param year The year for which to fetch data.
     * @param month The month (1-12) for which to fetch data.
     */
    private fun setupPieChartDataForMonth(year: Int, month: Int) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "setupPieChartDataForMonth: User not logged in, cannot load data.")
            Toast.makeText(this, "User not logged in. Cannot load dashboard data.", Toast.LENGTH_SHORT).show()
            pieChart.clear() // Clear any old data
            pieChart.setNoDataText("Please log in to view your dashboard.")
            pieChart.invalidate() // Redraw the chart
            return
        }

        Log.d(TAG, "setupPieChartDataForMonth: Fetching data for year: $year, month: $month, user: $userId")

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // Format start and end dates for Firestore query
        val startDate = "%04d-%02d-01".format(year, month)
        val endDate = LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.lastDayOfMonth())
            .toString()
        Log.d(TAG, "setupPieChartDataForMonth: Date range: $startDate to $endDate")

        // Fetch categories first, SPECIFIC TO THE USER
        firestore.collection("users").document(userId).collection("categories") // CHANGED: user-specific categories
            .get()
            .addOnSuccessListener { categoriesSnapshot ->
                Log.d(TAG, "setupPieChartDataForMonth: Categories fetched successfully.")

                if (categoriesSnapshot.isEmpty) {
                    Log.w(TAG, "setupPieChartDataForMonth: No categories found for this user.")
                    Toast.makeText(this, "No categories found for this user.", Toast.LENGTH_SHORT).show()
                    pieChart.clear()
                    pieChart.setNoDataText("No categories found.")
                    pieChart.invalidate()
                    return@addOnSuccessListener
                }

                val spendingData = mutableListOf<Triple<String, Float, Int>>()
                val categoryDocs = categoriesSnapshot.documents
                var processedCount = 0

                // For each category, fetch total spending for that month, SPECIFIC TO THE USER
                for (categoryDoc in categoryDocs) {
                    val categoryName = categoryDoc.getString("categoryName") ?: "Unknown"
                    val maxLimit = (categoryDoc.getLong("maxLimit") ?: 0).toInt()

                    // Query expenses for this category between startDate and endDate, SPECIFIC TO THE USER
                    firestore.collection("users").document(userId).collection("expenses") // CHANGED: user-specific expenses
                        .whereEqualTo("category", categoryName) // Assuming "category" field in expenses for the name
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                        .get()
                        .addOnSuccessListener { expensesSnapshot ->
                            Log.d(TAG, "setupPieChartDataForMonth: Expenses fetched for category: $categoryName")

                            var totalSpent = 0f
                            for (expenseDoc in expensesSnapshot.documents) {
                                val amount = (expenseDoc.getDouble("amount") ?: 0.0).toFloat()
                                totalSpent += amount
                            }
                            spendingData.add(Triple(categoryName, totalSpent, maxLimit))

                            processedCount++
                            // When all categories are processed, update pie chart
                            if (processedCount == categoryDocs.size) {
                                Log.d(TAG, "setupPieChartDataForMonth: All categories processed, updating pie chart.")
                                // Filter out categories with 0 spent to avoid showing them in the chart
                                val dataForChart = spendingData.filter { it.second > 0f }

                                if (dataForChart.isEmpty()) {
                                    Log.w(TAG, "setupPieChartDataForMonth: No expenses recorded for this month.")
                                    pieChart.clear()
                                    pieChart.setNoDataText("No expenses recorded for this month.")
                                    pieChart.invalidate()
                                    return@addOnSuccessListener
                                }

                                for ((name, spent, max) in dataForChart) {
                                    if (max <= 0) {
                                        // If maxLimit is 0 or less, we can't calculate percentage,
                                        // but we still want to show the spending.
                                        // You might want to handle this differently or display raw spent.
                                        // For now, if max is 0, we will assume it's still spending.
                                        entries.add(PieEntry(spent, name))
                                        colors.add(Color.LTGRAY) // Default color if no limit is set
                                    } else {
                                        entries.add(PieEntry(spent, name))
                                        val percent = (spent / max) * 100
                                        when {
                                            percent < 70 -> colors.add(Color.parseColor("#4CAF50")) // Green
                                            percent <= 100 -> colors.add(Color.parseColor("#FFC107")) // Yellow
                                            else -> colors.add(Color.parseColor("#F44336")) // Red (Over limit)
                                        }
                                    }
                                }

                                val dataSet = PieDataSet(entries, "")
                                dataSet.colors = colors
                                dataSet.valueTextColor = Color.WHITE
                                dataSet.valueTextSize = 14f

                                val pieData = PieData(dataSet)
                                pieChart.data = pieData
                                pieChart.description.isEnabled = false
                                pieChart.centerText = "Spending for ${monthSpinner.selectedItem}"
                                pieChart.setCenterTextSize(18f)
                                pieChart.setEntryLabelColor(Color.WHITE)
                                pieChart.setUsePercentValues(true)
                                pieChart.legend.isEnabled = false
                                pieChart.invalidate()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "setupPieChartDataForMonth: Failed to fetch expenses for category: ${e.message}", e)
                            Toast.makeText(this, "Failed to fetch expenses for category: ${e.message}", Toast.LENGTH_SHORT).show()
                            processedCount++ // Ensure count increments even on failure
                            if (processedCount == categoryDocs.size) {
                                // If all categories processed and failures occurred, update chart
                                // This might lead to an empty chart if all failed
                                pieChart.clear()
                                pieChart.setNoDataText("Error loading expense data.")
                                pieChart.invalidate()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "setupPieChartDataForMonth: Failed to fetch categories: ${e.message}", e)
                Toast.makeText(this, "Failed to fetch categories: ${e.message}", Toast.LENGTH_SHORT).show()
                pieChart.clear()
                pieChart.setNoDataText("Error loading category data.")
                pieChart.invalidate()
            }
    }

    /**
     * Exports the current pie chart as a PNG image to the external files directory.
     */
    private fun exportChartAsImage() {
        val bitmap = pieChart.chartBitmap
        val fileName = "chart_${System.currentTimeMillis()}.png"
        val filePath = getExternalFilesDir(null)?.absolutePath + "/" + fileName

        if (bitmap == null) {
            Log.w(TAG, "exportChartAsImage: No chart data to export.")
            Toast.makeText(this, "No chart data to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Log.d(TAG, "exportChartAsImage: Chart saved to: $filePath")
            Toast.makeText(this, "Chart saved to: $filePath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "exportChartAsImage: Export failed: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
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