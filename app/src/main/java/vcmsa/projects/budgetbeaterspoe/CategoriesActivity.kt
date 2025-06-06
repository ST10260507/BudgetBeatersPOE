package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CategoriesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimpleCategoryAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        enableEdgeToEdge()
        setupRecyclerView()
        loadCategoriesFromFirestore()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }

        setupBottomNav()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.categoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SimpleCategoryAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun loadCategoriesFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        // Load from user's categories subcollection
        firestore.collection("users").document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                val categories = mutableListOf<CategoryEntity>()
                for (document in result) {
                    val category = document.toObject(CategoryEntity::class.java)
                    categories.add(category)
                }
                adapter = SimpleCategoryAdapter(categories)
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
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