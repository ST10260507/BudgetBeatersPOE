package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

class ProgressDashboardActivity : AppCompatActivity() {

    // Declare UI components
    private lateinit var pieChart: PieChart
    private lateinit var db: AppDatabase
    private lateinit var monthSpinner: Spinner
    private lateinit var exportButton: Button
    private lateinit var backButton : Button

    // Setup activity when it's created
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress_dashboard)

        // Adjust window insets to ensure proper layout on devices with notches or edge-to-edge displays
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the database and UI components
        db = AppDatabase.getDatabase(this)
        pieChart = findViewById(R.id.pieChart)
        monthSpinner = findViewById(R.id.monthSpinner)
        exportButton = findViewById(R.id.exportBtn)
        backButton = findViewById(R.id.backBtn)

        // Set up the month spinner and bottom navigation
        setupMonthSpinner()
        setupBottomNav()

        // Set up listener to export the chart as an image
        exportButton.setOnClickListener {
            exportChartAsImage()
        }

        // Set up listener for the back button to close the activity
        backButton.setOnClickListener{
            finish()
        }
    }

    // Set up the month spinner with months of the year
    private fun setupMonthSpinner() {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        // Set up adapter for the spinner
        val adapter = ArrayAdapter(this, R.layout.spinner_item_white, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        // Set the current month as the default selection
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        monthSpinner.setSelection(currentMonth)

        // Set listener to update pie chart when a month is selected
        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedMonth = position + 1
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                setupPieChartDataForMonth(currentYear, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // Setup pie chart data for the selected month
    private fun setupPieChartDataForMonth(year: Int, month: Int) {
        lifecycleScope.launch {
            val entries = mutableListOf<PieEntry>()
            val colors = mutableListOf<Int>()

            // Define the start and end date for the selected month
            val startDate = "%04d-%02d-01".format(year, month)
            val endDate = LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.lastDayOfMonth())
                .toString()

            // Fetch the spending data for the selected month
            val categorySpending = withContext(Dispatchers.IO) {
                val allCategories = db.categoryDao().getAllCategories()
                val spendingData = mutableListOf<Triple<String, Float, Int>>()

                // Loop through each category and get the total spending
                for (cat in allCategories) {
                    val total = db.expenseDao()
                        .getTotalSpentForCategoryInRange(cat.categoryName, startDate, endDate)
                        ?: 0.0
                    spendingData.add(Triple(cat.categoryName, total.toFloat(), cat.maxLimit))
                }
                spendingData
            }

            // Loop through spending data to create PieChart entries
            for ((name, spent, max) in categorySpending) {
                if (max <= 0) continue
                entries.add(PieEntry(spent, name))

                // Assign colors based on spending percentage
                val percent = (spent / max) * 100
                when {
                    percent < 70 -> colors.add(Color.parseColor("#4CAF50")) // Green
                    percent in 70.0..100.0 -> colors.add(Color.parseColor("#FFC107")) // Yellow
                    else -> colors.add(Color.parseColor("#F44336")) // Red
                }
            }

            // Create a data set for the pie chart and set properties
            val dataSet = PieDataSet(entries, "")
            dataSet.colors = colors
            dataSet.valueTextColor = Color.WHITE
            dataSet.valueTextSize = 14f

            // Create the pie data and apply to the chart
            val pieData = PieData(dataSet)
            pieChart.data = pieData
            pieChart.description.isEnabled = false
            pieChart.centerText = "Spending for ${monthSpinner.selectedItem}"
            pieChart.setCenterTextSize(18f)
            pieChart.setEntryLabelColor(Color.WHITE)
            pieChart.setUsePercentValues(true)
            pieChart.legend.isEnabled = false
            pieChart.invalidate()  // Refresh the pie chart
        }
    }

    // Export the chart as an image file
    private fun exportChartAsImage() {
        // Get the bitmap of the pie chart
        val bitmap = pieChart.chartBitmap
        val fileName = "chart_${System.currentTimeMillis()}.png"
        val filePath = getExternalFilesDir(null)?.absolutePath + "/" + fileName
        val file = File(filePath)

        try {
            // Write the bitmap to the file
            val outputStream = FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Show success toast with file path
            Toast.makeText(this, "Chart saved to: $filePath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Setup bottom navigation to switch between fragments
    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    // Switch to Logout fragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    // Switch to Menu fragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    // Switch to Budgeting Guides fragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    // Switch to Awards fragment
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
