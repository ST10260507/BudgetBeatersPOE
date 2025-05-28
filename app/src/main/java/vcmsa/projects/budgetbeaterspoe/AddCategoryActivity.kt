package vcmsa.projects.budgetbeaterspoe

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityAddCategoryBinding

class AddCategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCategoryBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNav()

        binding.SaveBtn.setOnClickListener {
            val categoryName = binding.categoryNameInput.text.toString().trim()
            val description = binding.DescriptionInput.text.toString().trim()
            val maxLimitStr = binding.MaxLimitInput.text.toString().trim()
            val minLimitStr = binding.MinLimitInput.text.toString().trim()
            val userId = auth.currentUser?.uid ?: ""

            if (validateInput(categoryName, maxLimitStr, minLimitStr)) {
                val maxLimit = maxLimitStr.toInt()
                val minLimit = minLimitStr.toInt()

                val categoryData = hashMapOf(
                    "categoryName" to categoryName,
                    "description" to if (description.isNotEmpty()) description else null,
                    "maxLimit" to maxLimit,
                    "minLimit" to minLimit,
                    "userId" to userId
                )

                firestore.collection("categories")
                    .add(categoryData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@AddCategoryActivity,
                            "Category saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@AddCategoryActivity,
                            "Error saving category: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun validateInput(categoryName: String, maxLimit: String, minLimit: String): Boolean {
        var isValid = true

        if (categoryName.isEmpty()) {
            binding.categoryNameInput.error = "Category name required"
            isValid = false
        }

        if (maxLimit.isEmpty()) {
            binding.MaxLimitInput.error = "Max goal is required"
            isValid = false
        } else if (!maxLimit.all { it.isDigit() }) {
            binding.MaxLimitInput.error = "Only numbers allowed"
            isValid = false
        }

        if (minLimit.isEmpty()) {
            binding.MinLimitInput.error = "Min goal is required"
            isValid = false
        } else if (!minLimit.all { it.isDigit() }) {
            binding.MinLimitInput.error = "Only numbers allowed"
            isValid = false
        }

        return isValid
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