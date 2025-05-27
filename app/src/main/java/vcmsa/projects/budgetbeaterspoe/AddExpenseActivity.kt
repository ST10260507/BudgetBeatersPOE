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
    private var selectedImageUri: Uri? = null // To store the selected image URI

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

        loadCategoriesFromFirestore()

        binding.uploadImageView.setOnClickListener {
            openImagePicker()
        }

        binding.SaveBtn.setOnClickListener {
            val expenseName = binding.EXPENSENameInput3.text.toString().trim()
            val category = binding.CATEGORYSpinner.selectedItem?.toString() ?: ""
            val date = binding.DATEInput.text.toString().trim()
            val amount = binding.EXPENSEInput3.text.toString().trim().toDoubleOrNull()
            val description = binding.EXPENSEDescriptionInput.text.toString().trim()

            if (validateInput(expenseName, category, date, amount, description)) {
                lifecycleScope.launch {
                    try {
                        val imageUrl = selectedImageUri?.let { uri -> uploadImageToFirebase(uri) }
                        saveExpenseToFirestore(expenseName, category, date, amount!!, description, imageUrl)

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
        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("categories").get().await()
                val categoryNames = snapshot.documents.mapNotNull { it.getString("categoryName") }

                if (categoryNames.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_spinner_dropdown_item, categoryNames)
                    binding.CATEGORYSpinner.adapter = adapter
                } else {
                    Toast.makeText(this@AddExpenseActivity, "No categories found. Please add some first.", Toast.LENGTH_SHORT).show()
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

        if (category.isEmpty() || category == "Select Category") {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
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

    // Upload image to Firebase Storage and return its download URL
    private suspend fun uploadImageToFirebase(uri: Uri): String {
        val filename = "expenses_images/${UUID.randomUUID()}"
        val ref = storage.reference.child(filename)

        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // Save expense details to Firestore
    private suspend fun saveExpenseToFirestore(
        name: String,
        category: String,
        date: String,
        amount: Double,
        description: String,
        imageUrl: String?
    ) {
        val expenseData = hashMapOf(
            "name" to name,
            "category" to category,
            "date" to date,
            "amount" to amount,
            "description" to if (description.isNotEmpty()) description else null,
            "imageUrl" to imageUrl
        )

        firestore.collection("expenses").add(expenseData).await()
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
