package vcmsa.projects.budgetbeaterspoe

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private var selectedImageUri: Uri? = null // Variable to store the URI of the selected image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Handle window insets for adjusting UI padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load categories into the spinner from the database
        loadCategoriesFromDatabase()

        // Handle image upload functionality
        binding.uploadImageView.setOnClickListener {
            openImagePicker() // Open the image picker when the user clicks on the image view
        }

        // Handle the save button functionality
        binding.SaveBtn.setOnClickListener {
            // Get the data from the input fields
            val expenseName = binding.EXPENSENameInput3.text.toString().trim()
            val category = binding.CATEGORYSpinner.selectedItem?.toString() ?: ""
            val date = binding.DATEInput.text.toString().trim()
            val amount = binding.EXPENSEInput3.text.toString().trim().toDoubleOrNull()
            val description = binding.EXPENSEDescriptionInput.text.toString().trim()

            // Validate the input and save if valid
            if (validateInput(expenseName, category, date, amount, description)) {
                lifecycleScope.launch {
                    try {
                        // Insert the expense into the database
                        val database = AppDatabase.getDatabase(applicationContext)
                        database.expenseDao().insertExpense(
                            ExpenseEntity(
                                name = expenseName,
                                category = category,
                                date = date,
                                amount = amount ?: 0.0,
                                description = if (description.isNotEmpty()) description else null,
                                imagePath = selectedImageUri?.toString() // Store the image URI as a string
                            )
                        )
                        runOnUiThread {
                            // Show success message and close activity
                            Toast.makeText(this@AddExpenseActivity, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            // Show error message if saving failed
                            Toast.makeText(this@AddExpenseActivity, "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Date picker functionality
        binding.DATEInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Show the date picker dialog
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // Format the selected date and set it to the input field
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.DATEInput.setText(formattedDate)
            }, year, month, day)
            datePickerDialog.show()
        }

        // Set up the bottom navigation menu
        setupBottomNav()
    }

    // Function to load categories from the database into the spinner
    private fun loadCategoriesFromDatabase() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val categoryNames = withContext(Dispatchers.IO) {
                    // Get all categories from the database
                    db.categoryDao().getAllCategories().map { it.categoryName }
                }
                // If categories are found, update the spinner
                if (categoryNames.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_spinner_dropdown_item, categoryNames)
                    binding.CATEGORYSpinner.adapter = adapter
                } else {
                    // Show a toast if no categories are found
                    Toast.makeText(this@AddExpenseActivity, "No categories found. Please add some first.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Show error message if loading categories fails
                Toast.makeText(this@AddExpenseActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to open the image picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            // Request persistable read permission for the selected image
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    // Handle the result from the image picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            data?.data?.let { uri ->
                selectedImageUri = uri

                // Persist read permission for the selected image URI
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Preview the image using Glide
                Glide.with(this)
                    .load(uri)
                    .into(binding.uploadImageView)
            }
        }
    }

    // Function to validate user input before saving the expense
    private fun validateInput(expenseName: String, category: String, date: String, amount: Double?, description: String): Boolean {
        var isValid = true

        // Check if expense name is valid
        if (expenseName.isEmpty()) {
            binding.EXPENSENameInput3.error = "Expense name required"
            isValid = false
        } else binding.EXPENSENameInput3.error = null

        // Check if category is selected
        if (category.isEmpty() || category == "Select Category") {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if date is entered
        if (date.isEmpty()) {
            binding.DATEInput.error = "Date required"
            isValid = false
        } else binding.DATEInput.error = null

        // Check if amount is valid
        if (amount == null || amount <= 0) {
            binding.EXPENSEInput3.error = "Valid amount required"
            isValid = false
        } else binding.EXPENSEInput3.error = null

        // Check if description is entered
        if (description.isEmpty()) {
            binding.EXPENSEDescriptionInput.error = "Description required"
            isValid = false
        } else binding.EXPENSEDescriptionInput.error = null

        return isValid
    }

    companion object {
        const val IMAGE_PICK_CODE = 1000 // Request code for image picker activity
    }

    // Function to set up the bottom navigation
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
