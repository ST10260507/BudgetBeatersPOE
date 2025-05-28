package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
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

class PieChartActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        pieChart = findViewById(R.id.pieChart)

        findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        findViewById<Button>(R.id.btnDeleteCategory).setOnClickListener {
            startActivity(Intent(this, RemoveCategoryActivity::class.java))
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        setupPieChart()
    }

    private fun setupPieChart() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("categories")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val categories = querySnapshot.documents.map { doc ->
                    CategoryEntity(
                        id = doc.id,
                        categoryName = doc.getString("categoryName") ?: "",
                        maxLimit = (doc.getLong("maxLimit") ?: 0L).toInt(),
                        minLimit = (doc.getLong("minLimit") ?: 0L).toInt(),
                        userId = doc.getString("userId") ?: ""
                    )
                }

                if (categories.isNotEmpty()) {
                    val entries = categories.map {
                        PieEntry(it.maxLimit.toFloat(), it.categoryName)
                    }

                    val dataSet = PieDataSet(entries, "").apply {
                        colors = ColorTemplate.MATERIAL_COLORS.toList()
                    }

                    val data = PieData(dataSet)
                    pieChart.data = data

                    pieChart.apply {
                        isDrawHoleEnabled = true
                        holeRadius = 58f
                        setTransparentCircleRadius(61f)
                        animateY(1400)
                        description.isEnabled = false
                        invalidate()

                        legend.apply {
                            isEnabled = true
                            textSize = 14f
                            formSize = 18f
                            formToTextSpace = 10f
                            verticalAlignment = Legend.LegendVerticalAlignment.TOP
                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                            orientation = Legend.LegendOrientation.HORIZONTAL
                            yOffset = 40f
                            setDrawInside(false)
                        }

                        layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                            topMargin = 20
                        }
                    }
                } else {
                    pieChart.clear()
                    pieChart.invalidate()
                }
            }
            .addOnFailureListener { e ->
                pieChart.clear()
                pieChart.invalidate()
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