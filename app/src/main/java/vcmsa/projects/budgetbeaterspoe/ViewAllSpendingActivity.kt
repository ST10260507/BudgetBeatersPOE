package vcmsa.projects.budgetbeaterspoe

import android.app.DatePickerDialog
import android.os.Bundle
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
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityViewAllSpendingBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ViewAllSpendingActivity : AppCompatActivity() {

    // Binding and view variables
    private lateinit var binding: ActivityViewAllSpendingBinding
    private lateinit var barChart: BarChart
    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAllSpendingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize chart and date picker views
        initializeViews()
        // Set up date pickers for filtering
        setupDatePickers()
        // Configure chart appearance
        setupChartAppearance()
        // Configure button click for filtering
        setupFilterButton()
        // Load and display data
        loadData()
        // Set up bottom navigation
        setupBottomNav()
    }

    // Initialize view references
    private fun initializeViews() {
        barChart = binding.barChart
        etFromDate = binding.etFromDate
        etToDate = binding.etToDate
    }

    // Set up date picker dialogs for input fields
    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun createDatePicker(editText: EditText) {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    editText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Disable keyboard and attach date picker
        etFromDate.apply {
            setOnClickListener { createDatePicker(this) }
            keyListener = null
        }

        etToDate.apply {
            setOnClickListener { createDatePicker(this) }
            keyListener = null
        }
    }

    // Set up filter button to reload data
    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            loadData()
        }
    }

    // Configure chart layout and interaction settings
    private fun setupChartAppearance() {
        with(barChart) {
            description.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            setPinchZoom(true)
            isDragEnabled = true
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false
        }
    }

    // Load expense data from database and filter/display it
    private fun loadData() {
        val start = etFromDate.text.toString()
        val end = etToDate.text.toString()

        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val allExpenses = database.expenseDao().getAllExpenses()
                val filteredExpenses = if (start.isNotEmpty() && end.isNotEmpty()) {
                    filterExpensesByDate(allExpenses, start, end)
                } else {
                    allExpenses
                }

                runOnUiThread {
                    if (filteredExpenses.isEmpty()) {
                        showNoDataMessage()
                    } else {
                        displayChartData(filteredExpenses)
                    }
                }
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    // Filter expense list based on user-selected date range
    private fun filterExpensesByDate(expenses: List<ExpenseEntity>, start: String, end: String): List<ExpenseEntity> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dbDateFormat = SimpleDateFormat("yyyy-M-d", Locale.getDefault())

            val startDate = sdf.parse(start)!!
            val endDate = sdf.parse(end)!!

            expenses.filter { expense ->
                try {
                    val expenseDate = dbDateFormat.parse(expense.date)!!
                    expenseDate in startDate..endDate
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Display chart data using grouped expense categories
    private fun displayChartData(expenses: List<ExpenseEntity>) {
        val categoryMap = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        val entries = categoryMap.entries.mapIndexed { i, (cat, total) ->
            BarEntry(i.toFloat(), total.toFloat())
        }
        val labels = categoryMap.keys.toList()

        val dataSet = BarDataSet(entries, "Expenses by Category").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = android.graphics.Color.WHITE
            valueTextSize = 12f
        }

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            labelCount = labels.size
            textColor = android.graphics.Color.WHITE
        }

        barChart.legend.apply {
            textColor = android.graphics.Color.WHITE // ✅ Sets legend text color to white
        }

        barChart.data = BarData(dataSet)
        barChart.animateY(1000)
        barChart.invalidate()
    }

    // Show a toast if no expenses were found
    private fun showNoDataMessage() {
        Toast.makeText(this, "No expenses found in selected range", Toast.LENGTH_SHORT).show()
        barChart.clear()
        barChart.invalidate()
    }

    // Show a toast if an error occurs during data loading
    private fun showError(e: Exception) {
        runOnUiThread {
            Toast.makeText(
                this@ViewAllSpendingActivity,
                "Error loading data: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Set up navigation between fragments via bottom nav bar
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
