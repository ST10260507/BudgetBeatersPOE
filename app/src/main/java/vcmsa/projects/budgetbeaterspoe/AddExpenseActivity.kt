package vcmsa.projects.budgetbeaterspoe

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Map to store category names to Firestore document IDs
    private var categoryMap = mutableMapOf<String, String>()

    companion object {
        const val IMAGE_PICK_CODE = 1000
        const val REQUEST_IMAGE_CAPTURE = 1001
        const val CAMERA_PERMISSION_REQUEST = 1002
    }

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

        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermission()
        }

        binding.SaveBtn.setOnClickListener {
            val expenseName = binding.EXPENSENameInput3.text.toString().trim()
            val categoryName = binding.CATEGORYSpinner.selectedItem?.toString() ?: ""
            val date = binding.DATEInput.text.toString().trim()
            val amount = binding.EXPENSEInput3.text.toString().trim().toDoubleOrNull()
            val description = binding.EXPENSEDescriptionInput.text.toString().trim()
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (categoryMap[categoryName] == null && categoryName != "No categories") {
                Toast.makeText(this, "Could not find category ID. Please try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validateInput(expenseName, categoryName, date, amount, description)) {
                lifecycleScope.launch {
                    try {
                        // Convert image to compressed Base64
                        val base64Image = selectedImageUri?.let { uri ->
                            compressAndConvertToBase64(uri)
                        }

                        saveExpenseToFirestore(
                            name = expenseName,
                            categoryName = categoryName,
                            categoryId = categoryMap[categoryName] ?: "",
                            date = date,
                            amount = amount!!,
                            description = description,
                            base64Image = base64Image,
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
                        categoryMap[name] = doc.id
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
                    binding.SaveBtn.isEnabled = false
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
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        Glide.with(this).load(uri).into(binding.uploadImageView)
                    }
                }

                REQUEST_IMAGE_CAPTURE -> {
                    cameraImageUri?.let {
                        selectedImageUri = it
                        Glide.with(this).load(it).into(binding.uploadImageView)
                    }
                }
            }
        } else {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                cameraImageUri?.let { contentResolver.delete(it, null, null) }
                cameraImageUri = null
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

    // Compress image and convert to Base64
    private fun compressAndConvertToBase64(uri: Uri): String? {
        return try {
            // Step 1: Load bitmap with sampling
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(uri, 800, 800)
            }

            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Step 2: Compress to JPEG with quality control
            val outputStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()

            // Step 3: Check size before encoding
            if (byteArray.size > 900 * 1024) { // 900KB limit
                Toast.makeText(this, "Image too large (max 900KB)", Toast.LENGTH_SHORT).show()
                return null
            }

            // Convert to Base64
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Toast.makeText(this, "Image processing error: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Calculate sampling size to reduce memory usage
    private fun calculateInSampleSize(uri: Uri, reqWidth: Int, reqHeight: Int): Int {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private suspend fun saveExpenseToFirestore(
        name: String,
        categoryName: String,
        categoryId: String?,
        date: String,
        amount: Double,
        description: String,
        base64Image: String?,
        userId: String
    ) {
        val expenseData = hashMapOf(
            "name" to name,
            "category" to categoryName,
            "categoryId" to categoryId,
            "date" to date,
            "amount" to amount,
            "description" to description,
            "base64Image" to base64Image,  // Store Base64 string
            "userId" to userId
        )

        firestore.collection("users").document(userId)
            .collection("expenses").add(expenseData).await()
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

    // Camera functions
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "expense_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        cameraImageUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            cameraImageUri?.let {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, it)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            } ?: Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
        }
    }

    // Fix for the locale warning in date formatting
    private fun setupDatePicker() {
        binding.DATEInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // FIXED: Added Locale.US to prevent warnings
                val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.DATEInput.setText(formattedDate)
            }, year, month, day)
            datePickerDialog.show()
        }
    }
}