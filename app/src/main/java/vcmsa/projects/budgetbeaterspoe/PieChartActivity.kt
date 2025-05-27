package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PieChartActivity : AppCompatActivity() {

    // PieChart instance to display the chart
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        // Initialize PieChart
        pieChart = findViewById(R.id.pieChart)

        // Set up click listeners for category actions
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            // Open AddCategoryActivity when 'Add Category' button is clicked
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        findViewById<Button>(R.id.btnDeleteCategory).setOnClickListener {
            // Open RemoveCategoryActivity when 'Delete Category' button is clicked
            startActivity(Intent(this, RemoveCategoryActivity::class.java))
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            // Close the activity and return to the previous screen
            finish()
        }

        // Set up the bottom navigation
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the chart when returning to this activity
        setupPieChart()
    }

    // Function to set up the PieChart with data
    private fun setupPieChart() {
        val db = AppDatabase.getDatabase(this)

        // Launch a coroutine to fetch categories from the database
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                // Fetch all categories from the database
                db.categoryDao().getAllCategories()
            }

            // Check if categories are available
            if (categories.isNotEmpty()) {
                // Create PieEntries for each category
                val entries = categories.map {
                    PieEntry(it.maxLimit.toFloat(), it.categoryName)
                }

                // Create a PieDataSet from the entries and apply colors
                val dataSet = PieDataSet(entries, "").apply {
                    // Empty string to avoid the dataset title
                    colors = ColorTemplate.MATERIAL_COLORS.toList() // Use material design colors
                }

                // Create PieData from the PieDataSet
                val data = PieData(dataSet)
                pieChart.data = data

                // Customize the PieChart appearance
                pieChart.apply {
                    isDrawHoleEnabled = true
                    holeRadius = 58f // Set the hole radius for the center
                    setTransparentCircleRadius(61f) // Set the radius for the transparent circle
                    animateY(1400) // Animate the pie chart
                    description.isEnabled = false // Disable description text
                    invalidate() // Refresh the chart with new data

                    // Customize the legend of the PieChart
                    legend.apply {
                        isEnabled = true
                        textSize = 14f // Set the text size of legend entries
                        formSize = 18f // Set the size of the colored box next to the text
                        formToTextSpace = 10f

                        // Align the legend above the pie chart
                        verticalAlignment = Legend.LegendVerticalAlignment.TOP
                        horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                        orientation = Legend.LegendOrientation.HORIZONTAL // Change to vertical for multiple rows

                        // Add spacing between the legend and the chart
                        yOffset = 40f // Adjusted for better spacing
                        setDrawInside(false) // Prevent the legend from drawing inside the chart
                    }

                    // Adjust the layout parameters to add margin to the top
                    layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                        topMargin = 20 // Add margin at the top to avoid overlap with other UI elements
                    }
                }
            } else {
                // Clear the PieChart if no data is available
                pieChart.clear()
                pieChart.invalidate() // Refresh the chart to reflect the empty state
            }
        }
    }

    // Function to set up the bottom navigation bar
    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Handle logout navigation
                R.id.Logout -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                // Handle menu navigation
                R.id.Menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                // Handle budgeting guides navigation
                R.id.BudgetingGuides -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                // Handle awards navigation
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
