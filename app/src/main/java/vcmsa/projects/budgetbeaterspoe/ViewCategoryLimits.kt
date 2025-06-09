package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityViewCategoryLimitsBinding // Import your binding class

/**
 * Activity to view spending and limits for a selected category.
 * Users can select a category from a spinner, and then view a bar chart
 * comparing total spending, minimum limit, and maximum limit for that category.
 */
class ViewCategoryLimits : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "ViewCategoryLimits"

    private lateinit var binding: ActivityViewCategoryLimitsBinding
    private lateinit var barChart: BarChart

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // List to hold all CategoryEntity objects fetched for the current user
    private var allCategoryEntities = mutableListOf<CategoryEntity>()

    // Variable to hold the currently selected category from the spinner
    private var currentSelectedCategoryEntity: CategoryEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.")

        enableEdgeToEdge() // Enable edge-to-edge display
        binding = ActivityViewCategoryLimitsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        // Apply window insets to adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        initializeViews()
        setupSpinner()
        setupChartAppearance()
        setupButtons()
        setupBottomNav()

        // Initially hide the chart and back button until a category is selected and submitted
        barChart.visibility = View.GONE
        binding.backBtn.visibility = View.GONE
        Log.d(TAG, "onCreate: Initial view states set.")
        Log.d(TAG, "onCreate: All initial setups complete.")
    }

    /**
     * Initializes UI components from the binding object.
     */
    private fun initializeViews() {
        barChart = binding.barChart
        Log.d(TAG, "initializeViews: UI components initialized.")
    }

    /**
     * Populates the category spinner with category names fetched from Firestore.
     * It fetches `CategoryEntity` objects for the current user and uses their `categoryName`.
     */
    private fun setupSpinner() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in. Cannot load categories.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "setupSpinner: User not logged in. Aborting category load.")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "setupSpinner: Fetching categories for user ID: $userId")
                val snapshot = firestore.collection("users").document(userId).collection("categories")
                    .get()
                    .await()

                allCategoryEntities.clear()
                for (doc in snapshot.documents) {
                    val category = doc.toObject(CategoryEntity::class.java)
                    if (category != null && category.categoryName.isNotBlank()) {
                        allCategoryEntities.add(category)
                        Log.v(TAG, "setupSpinner: Added category: ${category.categoryName} (ID: ${category.id})")
                    }
                }

                // Extract only the category names for the spinner
                val categoryNamesForSpinner = allCategoryEntities.map { it.categoryName }.toMutableList()

                if (categoryNamesForSpinner.isEmpty()) {
                    Toast.makeText(this@ViewCategoryLimits, "No categories found. Add categories first.", Toast.LENGTH_LONG).show()
                    Log.i(TAG, "setupSpinner: No categories found for the user.")
                    // Optionally disable submit button if no categories
                    binding.SubmitBtn.isEnabled = false
                    return@launch
                }

                val adapter = ArrayAdapter(
                    this@ViewCategoryLimits,
                    android.R.layout.simple_spinner_item,
                    categoryNamesForSpinner
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                binding.CatSpinner.adapter = adapter

                // Set listener to update currentSelectedCategoryEntity when spinner item is selected
                binding.CatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedCategoryName = parent?.getItemAtPosition(position).toString()
                        currentSelectedCategoryEntity = allCategoryEntities.find { it.categoryName == selectedCategoryName }
                        Log.d(TAG, "setupSpinner: Selected category: $selectedCategoryName")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        currentSelectedCategoryEntity = null
                        Log.d(TAG, "setupSpinner: Nothing selected in spinner.")
                    }
                }
                Log.d(TAG, "setupSpinner: Spinner populated with ${categoryNamesForSpinner.size} categories.")

            } catch (e: Exception) {
                Toast.makeText(this@ViewCategoryLimits, "Error loading categories: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "setupSpinner: Failed to load categories: ${e.localizedMessage}", e)
            }
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
            setFitBars(true)             // Make the bars fit perfectly
            animateY(1000)               // Animate the chart along the Y-axis

            // Configure X-axis properties
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM // Place X-axis labels at the bottom
                granularity = 1f                     // Set granularity to 1 for distinct bars
                setDrawGridLines(false)              // Do not draw X-axis grid lines
                textColor = Color.WHITE              // Set X-axis label color to white
                textSize = 12f
                setLabelCount(3, false) // Ensure exactly 3 labels are displayed (Total, Min, Max)
            }
            // Configure Left Y-axis (values)
            axisLeft.apply {
                textColor = Color.WHITE
                textSize = 12f
                setDrawGridLines(true) // Draw grid lines for better readability of values
            }
            // Disable right Y-axis
            axisRight.isEnabled = false

            // Set legend text color to white
            legend.textColor = Color.WHITE
            legend.textSize = 14f
            legend.formSize = 10f // Size of the color squares in the legend

            setNoDataText("Select a category and click submit to view data.") // Message when no data is set
            setNoDataTextColor(Color.WHITE) // Color of no data text
        }
        Log.d(TAG, "setupChartAppearance: Chart appearance configured.")
    }

    /**
     * Sets up click listeners for the "Submit" and "Back" buttons.
     */
    private fun setupButtons() {
        binding.SubmitBtn.setOnClickListener {
            Log.d(TAG, "Submit button clicked.")
            currentSelectedCategoryEntity?.let { category ->
                loadCategoryDataAndExpenses(category)
            } ?: run {
                Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "SubmitBtn: No category selected.")
            }
        }

        binding.backBtn.setOnClickListener {
            Log.d(TAG, "Back button clicked. Navigating to ViewAllExpensesActivity.")
            // Assuming ViewAllExpensesActivity is the activity you want to go back to.
            // If it's a different activity, change this.
            val intent = Intent(this, ViewAllSpendingActivity::class.java) // Changed to ViewAllExpenses as per your files
            startActivity(intent)
            finish() // Finish this activity to prevent it from staying on the back stack
        }
        Log.d(TAG, "setupButtons: Button click listeners set.")
    }

    /**
     * Loads expense data and category limits for the selected category
     * and prepares it for display in the bar chart.
     * @param category The selected CategoryEntity containing limits.
     */
    private fun loadCategoryDataAndExpenses(category: CategoryEntity) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in. Cannot load data.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "loadCategoryDataAndExpenses: User not logged in. Aborting data load.")
            return
        }

        Log.d(TAG, "loadCategoryDataAndExpenses: Loading data for category: ${category.categoryName}")

        lifecycleScope.launch {
            try {
                // 1. Fetch all expenses for the user
                val expensesSnapshot = firestore.collection("users").document(userId).collection("expenses")
                    .get()
                    .await()

                val allExpenses = expensesSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ExpenseEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "loadCategoryDataAndExpenses: Error mapping expense document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                // 2. Calculate total spending for the selected category
                val totalSpending = allExpenses
                    .filter { it.category == category.categoryName } // Filter by the selected category name
                    .sumOf { it.amount }

                // Retrieve limits directly from the provided CategoryEntity
                val minLimit = category.minLimit
                val maxLimit = category.maxLimit

                Log.d(TAG, "loadCategoryDataAndExpenses: Category: ${category.categoryName}, Total Spent: $totalSpending, Min Limit: $minLimit, Max Limit: $maxLimit")

                runOnUiThread {
                    displayChartData(totalSpending, minLimit, maxLimit, category.categoryName)
                    barChart.visibility = View.VISIBLE // Make chart visible
                    binding.backBtn.visibility = View.VISIBLE // Make back button visible
                }

            } catch (e: Exception) {
                Toast.makeText(this@ViewCategoryLimits, "Error loading data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "loadCategoryDataAndExpenses: Failed to load data: ${e.localizedMessage}", e)
                barChart.clear() // Clear chart on error
                barChart.invalidate()
                barChart.visibility = View.GONE // Hide chart on error
                binding.backBtn.visibility = View.GONE // Hide back button on error
            }
        }
    }

    /**
     * Displays the total spending, minimum limit, and maximum limit in the BarChart.
     * @param totalSpent The calculated total spending for the category.
     * @param minLimit The minimum limit for the category.
     * @param maxLimit The maximum limit for the category.
     * @param categoryName The name of the category for the chart title.
     */
    private fun displayChartData(totalSpent: Double, minLimit: Int, maxLimit: Int, categoryName: String) {
        Log.d(TAG, "displayChartData: Plotting chart for $categoryName.")

        // Create BarEntry objects: Index 0 for Total Spent, 1 for Min Limit, 2 for Max Limit
        // Convert Int limits to Float for the chart
        val entries = ArrayList<BarEntry>().apply {
            add(BarEntry(0f, totalSpent.toFloat()))
            add(BarEntry(1f, minLimit.toFloat()))
            add(BarEntry(2f, maxLimit.toFloat()))
        }

        // Define labels for the X-axis
        val labels = listOf("Total Spent", "Min Limit", "Max Limit")

        // Set colors for the bars
        val colors = ArrayList<Int>().apply {
            add(ContextCompat.getColor(this@ViewCategoryLimits, R.color.chart_total_spent))
            add(ContextCompat.getColor(this@ViewCategoryLimits, R.color.chart_min_limit))
            add(ContextCompat.getColor(this@ViewCategoryLimits, R.color.chart_max_limit))
        }

        val dataSet = BarDataSet(entries, "Category Data").apply {
            setColors(colors) // Apply custom colors
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        barChart.data = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = labels.size // Ensure all labels are shown
        barChart.description.text = "Spending & Limits for $categoryName"
        barChart.description.textColor = Color.WHITE
        barChart.description.textSize = 14f

        barChart.animateY(1000) // Animate the chart
        barChart.invalidate()   // Refresh the chart
        Log.d(TAG, "displayChartData: Chart refreshed for $categoryName.")
    }

    /**
     * Sets up the bottom navigation view and defines the actions to be taken
     * when different menu items are selected (e.g., navigating to fragments).
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
                    Log.d(TAG, "BottomNav: Unrecognized item selected (ID: ${item.itemId}).")
                    false
                }
            }
        }
        Log.d(TAG, "setupBottomNav: Bottom navigation listener set.")
    }
}