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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityViewAllSpendingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ViewAllSpendingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAllSpendingBinding
    private lateinit var barChart: BarChart
    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAllSpendingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupDatePickers()
        setupChartAppearance()
        setupFilterButton()
        loadData()
        setupBottomNav()
    }

    private fun initializeViews() {
        barChart = binding.barChart
        etFromDate = binding.etFromDate
        etToDate = binding.etToDate
    }

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

        etFromDate.apply {
            setOnClickListener { createDatePicker(this) }
            keyListener = null
        }
        etToDate.apply {
            setOnClickListener { createDatePicker(this) }
            keyListener = null
        }
    }

    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            loadData()
        }
    }

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

    private fun loadData() {
        val start = etFromDate.text.toString()
        val end = etToDate.text.toString()

        lifecycleScope.launch {
            try {
                val expenses = fetchExpensesFromFirestore()

                val filteredExpenses = if (start.isNotEmpty() && end.isNotEmpty()) {
                    filterExpensesByDate(expenses, start, end)
                } else {
                    expenses
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

    private suspend fun fetchExpensesFromFirestore(): List<ExpenseEntity> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        val snapshot = firestore.collection("users").document(userId).collection("expenses")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                ExpenseEntity(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    category = doc.getString("category") ?: "",
                    amount = doc.getDouble("amount") ?: 0.0,
                    date = doc.getString("date") ?: "",
                    userId = doc.getString("userId") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    description = doc.getString("description") ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun filterExpensesByDate(expenses: List<ExpenseEntity>, start: String, end: String): List<ExpenseEntity> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.parse(start)!!
            val endDate = sdf.parse(end)!!

            expenses.filter { expense ->
                try {
                    val expenseDate = sdf.parse(expense.date)
                    expenseDate != null && expenseDate.time in startDate.time..endDate.time
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun displayChartData(expenses: List<ExpenseEntity>) {
        val categoryMap = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        val entries = categoryMap.entries.mapIndexed { i, (_, total) ->
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

        barChart.legend.textColor = android.graphics.Color.WHITE
        barChart.data = BarData(dataSet)
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun showNoDataMessage() {
        Toast.makeText(this, "No expenses found in selected range", Toast.LENGTH_SHORT).show()
        barChart.clear()
        barChart.invalidate()
    }

    private fun showError(e: Exception) {
        runOnUiThread {
            Toast.makeText(
                this@ViewAllSpendingActivity,
                "Error loading data: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
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