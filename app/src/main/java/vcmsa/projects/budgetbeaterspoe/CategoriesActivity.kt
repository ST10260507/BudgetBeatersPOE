package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    // Declare RecyclerView and adapter for displaying categories
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimpleCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        // Enable edge-to-edge display for immersive experience
        enableEdgeToEdge()

        // Setup RecyclerView to display categories
        setupRecyclerView()

        // Load categories from database
        loadCategories()

        // Apply window insets to adjust the UI when system bars (e.g., status bar) change
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up back button click listener to finish the activity when pressed
        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }

        // Setup bottom navigation bar for navigating between fragments
        setupBottomNav()
    }

    // Function to setup RecyclerView with layout manager and adapter
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.categoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SimpleCategoryAdapter(emptyList())  // Initialize with empty list
        recyclerView.adapter = adapter
    }

    // Function to load categories from the database using coroutine
    private fun loadCategories() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)  // Get database instance
            val categories = database.categoryDao().getAllCategories()  // Fetch all categories
            runOnUiThread {
                // Update the adapter with the fetched categories
                adapter = SimpleCategoryAdapter(categories)
                recyclerView.adapter = adapter
            }
        }
    }

    // Function to setup bottom navigation listener to handle fragment changes
    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    // Replace current fragment with LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    // Replace current fragment with Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    // Replace current fragment with BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    // Replace current fragment with AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> false  // Return false for unhandled menu items
            }
        }
    }
}
