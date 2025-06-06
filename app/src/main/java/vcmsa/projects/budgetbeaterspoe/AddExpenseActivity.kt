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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private var selectedImageUri: Uri? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Map to store category names to Firestore document IDs (needed for the spinner)
    private var categoryMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadCategoriesFromFirestore() // Load categories specific to the user

        binding.uploadImageView.setOnClickListener {
            openImagePicker()
        }

        binding.SaveBtn.setOnClickListener {
            val expenseName = binding.EXPENSENameInput3.text.toString().trim()
            val categoryName = binding.CATEGORYSpinner.selectedItem?.toString() ?: "" // Get category name
            val date = binding.DATEInput.text.toString().trim()
            val amount = binding.EXPENSEInput3.text.toString().trim().toDoubleOrNull()
            val description = binding.EXPENSEDescriptionInput.text.toString().trim()
            val userId = auth.currentUser?.uid // Get current user's UID

            // Get the selected category ID from the map
            val categoryId = categoryMap[categoryName] // Retrieve the ID based on the name

            if (userId == null) {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ensure a categoryId is found if a categoryName is selected
            if (categoryId == null && categoryName != "No categories") {
                Toast.makeText(this, "Could not find category ID. Please try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validateInput(expenseName, categoryName, date, amount, description)) {
                lifecycleScope.launch {
                    try {
                        val imageUrl = selectedImageUri?.let { uri -> uploadImageToFirebase(uri) }
                        saveExpenseToFirestore(
                            name = expenseName,
                            categoryName = categoryName,
                            categoryId = categoryId ?: "", // Pass the retrieved categoryId, default to empty if null
                            date = date,
                            amount = amount!!,
                            description = description,
                            imageUrl = imageUrl,
                            userId = userId
                        )

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddExpenseActivity, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddExpenseActivity, "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.DATEInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.DATEInput.setText(formattedDate)
            }, year, month, day)
            datePickerDialog.show()
        }

        setupBottomNav()
    }

    private fun loadCategoriesFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in. Cannot load categories.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).collection("categories")
                    .get().await()

                categoryMap.clear()
                val categoryNames = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    val name = doc.getString("categoryName")
                    if (name != null) {
                        categoryMap[name] = doc.id // Store name -> ID mapping
                        categoryNames.add(name)
                    }
                }

                if (categoryNames.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_spinner_dropdown_item, categoryNames)
                    binding.CATEGORYSpinner.adapter = adapter
                } else {
                    Toast.makeText(this@AddExpenseActivity, "No categories found. Please add some first.", Toast.LENGTH_SHORT).show()
                    val emptyAdapter = ArrayAdapter<String>(this@AddExpenseActivity, android.R.layout.simple_spinner_dropdown_item, listOf("No categories"))
                    binding.CATEGORYSpinner.adapter = emptyAdapter
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddExpenseActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Glide.with(this).load(uri).into(binding.uploadImageView)
            }
        }
    }

    private fun validateInput(expenseName: String, category: String, date: String, amount: Double?, description: String): Boolean {
        var isValid = true

        if (expenseName.isEmpty()) {
            binding.EXPENSENameInput3.error = "Expense name required"
            isValid = false
        } else binding.EXPENSENameInput3.error = null

        if (category.isEmpty() || category == "No categories") {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (date.isEmpty()) {
            binding.DATEInput.error = "Date required"
            isValid = false
        } else binding.DATEInput.error = null

        if (amount == null || amount <= 0) {
            binding.EXPENSEInput3.error = "Valid amount required"
            isValid = false
        } else binding.EXPENSEInput3.error = null

        if (description.isEmpty()) {
            binding.EXPENSEDescriptionInput.error = "Description required"
            isValid = false
        } else binding.EXPENSEDescriptionInput.error = null

        return isValid
    }

    private suspend fun uploadImageToFirebase(uri: Uri): String {
        val filename = "expenses_images/${UUID.randomUUID()}"
        val ref = storage.reference.child(filename)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private suspend fun saveExpenseToFirestore(
        name: String,
        categoryName: String,
        categoryId: String?, // Now explicitly nullable String?
        date: String,
        amount: Double,
        description: String,
        imageUrl: String?,
        userId: String
    ) {
        val expenseData = hashMapOf(
            "name" to name,
            "category" to categoryName, // Keep 'category' for the name as per ExpenseEntity
            "categoryId" to categoryId, // Add 'categoryId' for the ID
            "date" to date,
            "amount" to amount,
            "description" to if (description.isNotEmpty()) description else null,
            "imageUrl" to imageUrl,
            "userId" to userId
        )

        firestore.collection("users").document(userId).collection("expenses").add(expenseData).await()
    }

    companion object {
        const val IMAGE_PICK_CODE = 1000
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