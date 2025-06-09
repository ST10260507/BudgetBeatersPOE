package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast // Import for displaying user messages
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity responsible for displaying a pie chart of user's budget categories.
 * It fetches category data from Firebase Firestore and visualizes their maximum limits.
 */
class PieChartActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "PieChartActivity"

    // PieChart view instance
    private lateinit var pieChart: PieChart

    // Firestore database instance
    private val firestore = FirebaseFirestore.getInstance()

    // Firebase Authentication instance
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation
        setContentView(R.layout.activity_pie_chart)

        // Initialize the PieChart view
        pieChart = findViewById(R.id.pieChart)
        Log.d(TAG, "onCreate: PieChart view initialized.")

        // Set click listener for the "Add Category" button
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            Log.d(TAG, "btnAddCategory clicked: Launching AddCategoryActivity.") // Log button click
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        // Set click listener for the "Delete Category" button
        findViewById<Button>(R.id.btnDeleteCategory).setOnClickListener {
            Log.d(TAG, "btnDeleteCategory clicked: Launching RemoveCategoryActivity.") // Log button click
            startActivity(Intent(this, RemoveCategoryActivity::class.java))
        }

        // Set click listener for the "Back" button
        findViewById<Button>(R.id.backBtn).setOnClickListener {
            Log.d(TAG, "backBtn clicked: Finishing activity.") // Log button click
            finish() // Close the current activity
        }

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity resumed. Setting up Pie Chart.") // Log activity resume
        // Set up the pie chart data and appearance when the activity comes to foreground
        setupPieChart()
    }

    /**
     * Fetches category data from Firestore for the current user and populates the PieChart.
     */
    private fun setupPieChart() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "setupPieChart: User not logged in. Cannot load categories for chart.") // Log warning
            Toast.makeText(this, "User not logged in. Please log in to view budget.", Toast.LENGTH_SHORT).show()
            pieChart.clear() // Clear any existing data
            pieChart.invalidate() // Redraw the chart
            return // Exit if user is not authenticated
        }

        Log.d(TAG, "setupPieChart: Fetching categories for user ID: $userId")

        // Load from user's categories subcollection in Firestore
        firestore.collection("users").document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Map Firestore documents to CategoryEntity objects
                val categories = querySnapshot.documents.map { doc ->
                    CategoryEntity(
                        id = doc.id,
                        categoryName = doc.getString("categoryName") ?: "Unknown Category", // Default name if null
                        maxLimit = (doc.getLong("maxLimit") ?: 0L).toInt(), // Default to 0 if null
                        minLimit = (doc.getLong("minLimit") ?: 0L).toInt(), // Default to 0 if null
                        userId = doc.getString("userId") ?: ""
                    )
                }
                Log.d(TAG, "setupPieChart: Successfully fetched ${categories.size} categories.")

                if (categories.isNotEmpty()) {
                    // Create PieEntry objects from categories (using maxLimit as value)
                    val entries = categories.map {
                        PieEntry(it.maxLimit.toFloat(), it.categoryName)
                    }
                    Log.d(TAG, "setupPieChart: Created ${entries.size} PieEntries.")

                    // Create a PieDataSet from the entries
                    val dataSet = PieDataSet(entries, "").apply {
                        // Set colors for the slices using predefined Material colors
                        colors = ColorTemplate.MATERIAL_COLORS.toList()
                        valueTextSize = 12f // Set text size for values on slices
                        valueFormatter = com.github.mikephil.charting.formatter.PercentFormatter(pieChart) // Format values as percentages
                    }

                    // Create PieData from the dataSet
                    val data = PieData(dataSet)
                    pieChart.data = data // Set the data to the pie chart

                    // Configure general appearance and animations of the PieChart
                    pieChart.apply {
                        isDrawHoleEnabled = true // Enable drawing a hole in the middle
                        holeRadius = 58f // Size of the inner hole
                        setTransparentCircleRadius(61f) // Size of the transparent circle around the hole
                        animateY(1400) // Animate chart slices from bottom to top
                        description.isEnabled = false // Disable default description text
                        setUsePercentValues(true) // Display values as percentages
                        setEntryLabelTextSize(10f) // Size of category labels on slices
                        setEntryLabelColor(android.graphics.Color.BLACK) // Color of category labels
                        invalidate() // Redraw the chart to apply changes
                        Log.d(TAG, "setupPieChart: PieChart data set and configured. Chart invalidated.")

                        // Configure the legend (category labels and colors outside the chart)
                        legend.apply {
                            isEnabled = true // Enable the legend
                            textSize = 14f // Text size of legend entries
                            formSize = 18f // Size of the colored form/square next to legend text
                            formToTextSpace = 10f // Space between form and text
                            verticalAlignment = Legend.LegendVerticalAlignment.TOP // Align legend to the top
                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER // Center legend horizontally
                            orientation = Legend.LegendOrientation.HORIZONTAL // Arrange legend items horizontally
                            yOffset = 40f // Offset from the top edge
                            setDrawInside(false) // Draw legend outside the chart
                        }

                        // Adjust layout parameters for top margin if needed (example)
                        layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                            topMargin = 20
                        }
                    }
                } else {
                    // If no categories are found, clear the chart and invalidate it
                    Log.d(TAG, "setupPieChart: No categories found. Clearing chart.")
                    pieChart.clear()
                    pieChart.invalidate()
                    Toast.makeText(this, "No categories found to display in chart.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Log and show error if fetching categories fails
                Log.e(TAG, "setupPieChart: Error loading categories for pie chart: ${e.message}", e)
                Toast.makeText(this, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
                pieChart.clear() // Clear chart on error
                pieChart.invalidate() // Redraw empty chart
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