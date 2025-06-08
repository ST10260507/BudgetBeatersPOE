package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import java.util.*

// Activity to display and handle category income related operations
class CategoryIncomeActivity : AppCompatActivity() {

    private lateinit var categorySpinner: Spinner
    private lateinit var fromDateInput: EditText
    private lateinit var toDateInput: EditText
    private lateinit var submitBtn: Button
    private lateinit var totalTextView: TextView

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_income)

        categorySpinner = findViewById(R.id.categorySpinner)
        fromDateInput = findViewById(R.id.FromDateInput)
        toDateInput = findViewById(R.id.ToDateInput)
        submitBtn = findViewById(R.id.submitBtn)
        totalTextView = findViewById(R.id.totalTextView)

        loadCategories()

        fromDateInput.setOnClickListener { showDatePicker(fromDateInput) }
        toDateInput.setOnClickListener { showDatePicker(toDateInput) }

        submitBtn.setOnClickListener { handleSubmission() }

        setupBottomNav()
    }

    private fun loadCategories() {
        val userId = auth.currentUser?.uid // Get the current user's ID
        if (userId == null) {
            Toast.makeText(this, "User not logged in. Cannot load categories.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId).collection("categories") // Modified to be user-specific
            .get()
            .addOnSuccessListener { result ->
                // Assuming "categoryName" is the field in your category documents
                val categoryNames = result.documents.mapNotNull { it.getString("categoryName") }
                    .distinct() // Ensure unique category names
                    .sorted() // Sort alphabetically

                if (categoryNames.isEmpty()) {
                    Toast.makeText(this, "No categories found for this user.", Toast.LENGTH_SHORT).show()
                    // Optionally, disable the spinner or show a placeholder
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load categories: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            editText.setText(formattedDate)
        }

        DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun handleSubmission() {
        val selectedCategory = categorySpinner.selectedItem?.toString()
        val fromDate = fromDateInput.text.toString()
        val toDate = toDateInput.text.toString()

        if (selectedCategory.isNullOrBlank()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            return
        }

        if (fromDate.isBlank() || toDate.isBlank()) {
            Toast.makeText(this, "Please select both dates.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in. Cannot retrieve data.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId).collection("expenses")
            .whereEqualTo("category", selectedCategory) // <-- CHANGED from "categoryName" to "category"
            .whereGreaterThanOrEqualTo("date", fromDate)
            .whereLessThanOrEqualTo("date", toDate)
            .get()
            .addOnSuccessListener { result ->
                val total = result.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                val display = if (total > 0)
                    "Total Spent: R%.2f".format(total)
                else
                    "No expenses found for this period in this category."

                totalTextView.text = display
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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