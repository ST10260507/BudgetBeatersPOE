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
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

class ProgressDashboardActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var monthSpinner: Spinner
    private lateinit var exportButton: Button
    private lateinit var backButton: Button

    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pieChart = findViewById(R.id.pieChart)
        monthSpinner = findViewById(R.id.monthSpinner)
        exportButton = findViewById(R.id.exportBtn)
        backButton = findViewById(R.id.backBtn)

        setupMonthSpinner()
        setupBottomNav()

        exportButton.setOnClickListener {
            exportChartAsImage()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupMonthSpinner() {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val adapter = ArrayAdapter(this, R.layout.spinner_item_white, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        monthSpinner.setSelection(currentMonth)

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedMonth = position + 1
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                setupPieChartDataForMonth(currentYear, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupPieChartDataForMonth(year: Int, month: Int) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        val startDate = "%04d-%02d-01".format(year, month)
        val endDate = LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.lastDayOfMonth())
            .toString()

        // Fetch categories first
        firestore.collection("categories")
            .get()
            .addOnSuccessListener { categoriesSnapshot ->

                if (categoriesSnapshot.isEmpty) {
                    Toast.makeText(this, "No categories found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val spendingData = mutableListOf<Triple<String, Float, Int>>()
                val categoryDocs = categoriesSnapshot.documents
                var processedCount = 0

                // For each category, fetch total spending for that month
                for (categoryDoc in categoryDocs) {
                    val categoryName = categoryDoc.getString("categoryName") ?: "Unknown"
                    val maxLimit = (categoryDoc.getLong("maxLimit") ?: 0).toInt()

                    // Query expenses for this category between startDate and endDate
                    firestore.collection("expenses")
                        .whereEqualTo("categoryName", categoryName)
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                        .get()
                        .addOnSuccessListener { expensesSnapshot ->

                            var totalSpent = 0f
                            for (expenseDoc in expensesSnapshot.documents) {
                                val amount = (expenseDoc.getDouble("amount") ?: 0.0).toFloat()
                                totalSpent += amount
                            }
                            spendingData.add(Triple(categoryName, totalSpent, maxLimit))

                            processedCount++
                            // When all categories are processed, update pie chart
                            if (processedCount == categoryDocs.size) {
                                for ((name, spent, max) in spendingData) {
                                    if (max <= 0) continue
                                    entries.add(PieEntry(spent, name))

                                    val percent = (spent / max) * 100
                                    when {
                                        percent < 70 -> colors.add(Color.parseColor("#4CAF50")) // Green
                                        percent in 70.0..100.0 -> colors.add(Color.parseColor("#FFC107")) // Yellow
                                        else -> colors.add(Color.parseColor("#F44336")) // Red
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
                            Toast.makeText(this, "Failed to fetch expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportChartAsImage() {
        val bitmap = pieChart.chartBitmap
        val fileName = "chart_${System.currentTimeMillis()}.png"
        val filePath = getExternalFilesDir(null)?.absolutePath + "/" + fileName
        val file = File(filePath)

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Toast.makeText(this, "Chart saved to: $filePath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
