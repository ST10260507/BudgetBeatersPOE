package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth // Added this import
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityRemoveCategoryBinding

class RemoveCategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRemoveCategoryBinding

    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()
    // FirebaseAuth instance
    private val auth = FirebaseAuth.getInstance() // Added FirebaseAuth instance

    // Map to store category names to Firestore document IDs
    private var categoryMap = mutableMapOf<String, String>()

    // Selected Firestore document ID of the category to delete
    private var selectedCategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoveCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        loadCategories()
        setupButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNav()
    }

    private fun loadCategories() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            // Optionally, redirect to login or handle unauthenticated state
            return
        }
        val userId = currentUser.uid

        // Fetch categories from the subcollection under the current user's document
        firestore.collection("users").document(userId).collection("categories")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Clear the map in case of refresh
                categoryMap.clear()

                for (doc in querySnapshot.documents) {
                    val name = doc.getString("categoryName") ?: "Unnamed"
                    val id = doc.id
                    categoryMap[name] = id
                }

                val categoryNames = categoryMap.keys.toList()

                // Check if categories are loaded before setting up spinner
                if (categoryNames.isNotEmpty()) {
                    val adapter = ArrayAdapter(this, R.layout.spinner_item_white, categoryNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.categoriesSpinner.adapter = adapter

                    binding.categoriesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View,
                            position: Int,
                            id: Long
                        ) {
                            val selectedName = categoryNames[position]
                            selectedCategoryId = categoryMap[selectedName]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            selectedCategoryId = null
                        }
                    }
                } else {
                    // Handle case where no categories are found for the user
                    Toast.makeText(this, "No categories found for this user.", Toast.LENGTH_SHORT).show()
                    val emptyAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_white, listOf("No categories"))
                    binding.categoriesSpinner.adapter = emptyAdapter
                    selectedCategoryId = null // Ensure no category is selected
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load categories: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupButtons() {
        binding.ConfirmDelBtn.setOnClickListener {
            deleteSelectedCategory()
        }

        binding.CancelDelBtn.setOnClickListener {
            finish()
        }
    }

    private fun deleteSelectedCategory() {
        val id = selectedCategoryId
        if (id == null) {
            Toast.makeText(this, "No category selected", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            // Optionally, redirect to login or handle unauthenticated state
            return
        }
        val userId = currentUser.uid

        // Delete from the subcollection under the current user's document
        firestore.collection("users").document(userId).collection("categories").document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                loadCategories() // Reload categories after successful deletion
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete category: ${e.message}", Toast.LENGTH_LONG).show()
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