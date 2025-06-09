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
import android.util.Log // Import for logging
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
import java.text.SimpleDateFormat // Import SimpleDateFormat for date formatting if needed elsewhere

class AddExpenseActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "AddExpenseActivity"

    // ViewBinding instance for accessing layout elements
    private lateinit var binding: ActivityAddExpenseBinding

    // URI for image selected from gallery
    private var selectedImageUri: Uri? = null

    // URI for image captured by camera
    private var cameraImageUri: Uri? = null

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    // Firebase Auth instance for getting current user information
    private val auth = FirebaseAuth.getInstance()

    // Map to store category names as keys and Firestore document IDs as values
    private var categoryMap = mutableMapOf<String, String>()

    // Companion object to hold constants used in the activity
    companion object {
        const val IMAGE_PICK_CODE = 1000 // Request code for picking an image from gallery
        const val REQUEST_IMAGE_CAPTURE = 1001 // Request code for capturing an image with camera
        const val CAMERA_PERMISSION_REQUEST = 1002 // Request code for camera permission
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Initialize ViewBinding
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable edge-to-edge display for a more immersive experience
        enableEdgeToEdge()

        // Set up window insets listener to handle system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the main view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        // Load categories from Firestore to populate the spinner
        loadCategoriesFromFirestore()
        Log.d(TAG, "onCreate: Initiated loading categories.")

        // Set click listener for the upload image view (gallery icon)
        binding.uploadImageView.setOnClickListener {
            Log.d(TAG, "uploadImageView clicked: Opening image picker.")
            openImagePicker()
        }

        // Set click listener for the "Take Photo" button
        binding.btnTakePhoto.setOnClickListener {
            Log.d(TAG, "btnTakePhoto clicked: Checking camera permission.")
            checkCameraPermission()
        }

        // Set click listener for the Save button
        binding.SaveBtn.setOnClickListener {
            Log.d(TAG, "Save button clicked. Attempting to save expense.")

            // Retrieve input values from UI elements
            val expenseName = binding.EXPENSENameInput3.text.toString().trim()
            val categoryName = binding.CATEGORYSpinner.selectedItem?.toString() ?: ""
            val date = binding.DATEInput.text.toString().trim()
            val amount = binding.EXPENSEInput3.text.toString().trim().toDoubleOrNull() // Convert to Double
            val description = binding.EXPENSEDescriptionInput.text.toString().trim()
            val userId = auth.currentUser?.uid // Get current user's ID

            Log.d(TAG, "Collected expense data: Name='$expenseName', Category='$categoryName', Date='$date', Amount=$amount, Description='$description', UserId='$userId'")

            // Check if user is logged in
            if (userId == null) {
                Log.w(TAG, "User ID is null. Cannot save expense.")
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Exit the click listener
            }

            // Validate if selected category exists in the map or if it's the "No categories" placeholder
            if (categoryMap[categoryName] == null && categoryName != "No categories") {
                Log.e(TAG, "Could not find category ID for '$categoryName'. categoryMap: $categoryMap")
                Toast.makeText(this, "Could not find category ID. Please try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Exit the click listener
            }

            // Perform input validation
            if (validateInput(expenseName, categoryName, date, amount, description)) {
                Log.d(TAG, "Input validation successful.")

                // Launch a coroutine to handle async operations like image compression and Firestore save
                lifecycleScope.launch {
                    try {
                        // Convert selected image to compressed Base64 string in a background thread
                        val base64Image = selectedImageUri?.let { uri ->
                            Log.d(TAG, "Image URI selected: $uri. Starting compression.")
                            withContext(Dispatchers.IO) { // Perform heavy image processing on IO dispatcher
                                compressAndConvertToBase64(uri)
                            }
                        }

                        // Save expense data to Firestore
                        saveExpenseToFirestore(
                            name = expenseName,
                            categoryName = categoryName,
                            categoryId = categoryMap[categoryName], // Get Firestore category ID
                            date = date,
                            amount = amount!!, // '!!' is safe here because amount is validated to be non-null
                            description = description,
                            base64Image = base64Image,
                            userId = userId
                        )
                        Log.d(TAG, "Expense data sent to Firestore for saving.")

                        // Show success message on the main thread and finish activity
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddExpenseActivity, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Expense saved successfully. Finishing activity.")
                            finish()
                        }
                    } catch (e: Exception) {
                        // Handle any exceptions during image processing or Firestore save
                        Log.e(TAG, "Error saving expense: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddExpenseActivity, "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Log.d(TAG, "Input validation failed. Cannot save expense.")
            }
        }

        // Set click listener for the Date input field to show DatePickerDialog
        binding.DATEInput.setOnClickListener {
            Log.d(TAG, "DATEInput clicked: Opening DatePickerDialog.")
            setupDatePicker() // Call the date picker setup function
        }

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")
    }

    /**
     * Loads categories from Firestore for the current user and populates the category spinner.
     */
    private fun loadCategoriesFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "loadCategoriesFromFirestore: User not logged in, cannot load categories.")
            Toast.makeText(this, "User not logged in. Cannot load categories.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "loadCategoriesFromFirestore: Fetching categories for user: $userId")
        lifecycleScope.launch {
            try {
                // Fetch category documents from Firestore
                val snapshot = firestore.collection("users").document(userId).collection("categories")
                    .get().await() // Await the result from Firestore

                categoryMap.clear() // Clear existing map before populating
                val categoryNames = mutableListOf<String>() // List to hold category names for the spinner

                // Iterate through fetched documents
                for (doc in snapshot.documents) {
                    val name = doc.getString("categoryName")
                    if (name != null) {
                        categoryMap[name] = doc.id // Store category name and its document ID
                        categoryNames.add(name) // Add name to list for spinner
                        Log.d(TAG, "loadCategoriesFromFirestore: Found category: '$name' with ID: '${doc.id}'")
                    } else {
                        Log.w(TAG, "loadCategoriesFromFirestore: Category document with null 'categoryName' found: ${doc.id}")
                    }
                }

                // Check if any categories were found
                if (categoryNames.isNotEmpty()) {
                    Log.d(TAG, "loadCategoriesFromFirestore: ${categoryNames.size} categories loaded. Populating spinner.")
                    // Create and set adapter for the spinner with loaded category names
                    val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_spinner_dropdown_item, categoryNames)
                    binding.CATEGORYSpinner.adapter = adapter
                    binding.SaveBtn.isEnabled = true // Enable save button if categories are present
                } else {
                    Log.d(TAG, "loadCategoriesFromFirestore: No categories found for user. Displaying 'No categories' placeholder.")
                    // If no categories, display a placeholder and disable the save button
                    Toast.makeText(this@AddExpenseActivity, "No categories found. Please add some first.", Toast.LENGTH_SHORT).show()
                    val emptyAdapter = ArrayAdapter<String>(this@AddExpenseActivity, android.R.layout.simple_spinner_dropdown_item, listOf("No categories"))
                    binding.CATEGORYSpinner.adapter = emptyAdapter
                    binding.SaveBtn.isEnabled = false // Disable save button
                }
            } catch (e: Exception) {
                // Log and show error if fetching categories fails
                Log.e(TAG, "loadCategoriesFromFirestore: Error loading categories: ${e.message}", e)
                Toast.makeText(this@AddExpenseActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.SaveBtn.isEnabled = false // Disable save button on error
            }
        }
    }

    /**
     * Opens the device's image picker to select an image from the gallery.
     */
    private fun openImagePicker() {
        // Create an intent to pick a document (image)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE) // Make it browsable in a file system
            type = "image/*" // Specify that we want image files
            // Grant read URI permission and persistable URI permission for long-term access
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        Log.d(TAG, "openImagePicker: Starting activity for result with IMAGE_PICK_CODE.")
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    /**
     * Handles the result of activities started with `startActivityForResult`,
     * specifically for image selection and camera capture.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (resultCode == RESULT_OK) { // Check if the operation was successful
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    // Handle image picked from gallery
                    data?.data?.let { uri ->
                        selectedImageUri = uri // Store the selected image URI
                        Log.d(TAG, "IMAGE_PICK_CODE: Image selected, URI: $uri")
                        // Take persistable URI permission to ensure future access
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        // Load and display the selected image using Glide
                        Glide.with(this).load(uri).into(binding.uploadImageView)
                        Log.d(TAG, "IMAGE_PICK_CODE: Image loaded into ImageView.")
                    } ?: run {
                        Log.w(TAG, "IMAGE_PICK_CODE: Data or URI is null.")
                        Toast.makeText(this, "Failed to get image.", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    // Handle image captured by camera
                    cameraImageUri?.let {
                        selectedImageUri = it // Store the camera image URI
                        Log.d(TAG, "REQUEST_IMAGE_CAPTURE: Image captured, URI: $it")
                        // Load and display the captured image using Glide
                        Glide.with(this).load(it).into(binding.uploadImageView)
                        Log.d(TAG, "REQUEST_IMAGE_CAPTURE: Image loaded into ImageView.")
                    } ?: run {
                        Log.w(TAG, "REQUEST_IMAGE_CAPTURE: cameraImageUri is null after capture.")
                        Toast.makeText(this, "Failed to get camera image.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else { // If the operation was cancelled or failed
            Log.d(TAG, "onActivityResult: Operation cancelled or failed for request code: $requestCode")
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // If camera capture was cancelled, delete the temporary file created
                cameraImageUri?.let {
                    Log.d(TAG, "REQUEST_IMAGE_CAPTURE cancelled: Deleting temporary image file: $it")
                    contentResolver.delete(it, null, null)
                }
                cameraImageUri = null // Clear the URI
            }
            Toast.makeText(this, "Operation cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Validates the user input fields before saving an expense.
     * Sets error messages on respective input fields if validation fails.
     * @return true if all inputs are valid, false otherwise.
     */
    private fun validateInput(
        expenseName: String,
        category: String,
        date: String,
        amount: Double?,
        description: String
    ): Boolean {
        Log.d(TAG, "validateInput: Starting validation...")
        var isValid = true

        // Validate Expense Name
        if (expenseName.isEmpty()) {
            binding.EXPENSENameInput3.error = "Expense name required"
            Log.d(TAG, "Validation failed: Expense name is empty.")
            isValid = false
        } else {
            binding.EXPENSENameInput3.error = null // Clear error if valid
        }

        // Validate Category Selection
        if (category.isEmpty() || category == "No categories") {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Validation failed: Category is empty or 'No categories'.")
            isValid = false
        }
        // No direct error setting on spinner, Toast is used.

        // Validate Date
        if (date.isEmpty()) {
            binding.DATEInput.error = "Date required"
            Log.d(TAG, "Validation failed: Date is empty.")
            isValid = false
        } else {
            binding.DATEInput.error = null // Clear error if valid
        }

        // Validate Amount
        if (amount == null || amount <= 0) {
            binding.EXPENSEInput3.error = "Valid amount required"
            Log.d(TAG, "Validation failed: Amount is null or not positive.")
            isValid = false
        } else {
            binding.EXPENSEInput3.error = null // Clear error if valid
        }

        // Validate Description
        if (description.isEmpty()) {
            binding.EXPENSEDescriptionInput.error = "Description required"
            Log.d(TAG, "Validation failed: Description is empty.")
            isValid = false
        } else {
            binding.EXPENSEDescriptionInput.error = null // Clear error if valid
        }

        Log.d(TAG, "validateInput: Validation complete. isValid = $isValid")
        return isValid
    }

    /**
     * Compresses a given image URI and converts it into a Base64 encoded string.
     * @param uri The URI of the image to compress.
     * @return The Base64 string of the compressed image, or null if an error occurs or image is too large.
     */
    private fun compressAndConvertToBase64(uri: Uri): String? {
        Log.d(TAG, "compressAndConvertToBase64: Starting image compression for URI: $uri")
        return try {
            // Step 1: Load bitmap with sampling to reduce initial memory footprint
            val options = BitmapFactory.Options().apply {
                // Calculate inSampleSize to load a scaled down version of the image
                inSampleSize = calculateInSampleSize(uri, 800, 800) // Target max 800x800 resolution
                Log.d(TAG, "compressAndConvertToBase64: Calculated inSampleSize: $inSampleSize")
            }

            // Open input stream from URI and decode bitmap
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close() // Close the input stream
            Log.d(TAG, "compressAndConvertToBase64: Bitmap decoded. Dimensions: ${bitmap?.width}x${bitmap?.height}")

            if (bitmap == null) {
                Log.e(TAG, "compressAndConvertToBase64: Bitmap decoding resulted in null.")
                Toast.makeText(this, "Failed to decode image bitmap.", Toast.LENGTH_SHORT).show()
                return null
            }

            // Step 2: Compress to JPEG with quality control
            val outputStream = ByteArrayOutputStream()
            // Compress bitmap to JPEG format with 70% quality (0-100, 100 being max quality)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            Log.d(TAG, "compressAndConvertToBase64: Image compressed to JPEG, size: ${byteArray.size / 1024} KB")

            // Step 3: Check size before encoding (Firebase has limits on document size)
            val maxSizeKB = 900 // Define maximum allowed size in KB
            if (byteArray.size > maxSizeKB * 1024) { // Convert KB to bytes
                Log.w(TAG, "compressAndConvertToBase64: Compressed image is too large (${byteArray.size / 1024}KB > ${maxSizeKB}KB).")
                Toast.makeText(this, "Image too large (max ${maxSizeKB}KB). Please choose a smaller image.", Toast.LENGTH_LONG).show()
                return null
            }

            // Convert the byte array to a Base64 string
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.d(TAG, "compressAndConvertToBase64: Image converted to Base64 string.")
            base64String
        } catch (e: Exception) {
            // Log and show error if any exception occurs during image processing
            Log.e(TAG, "compressAndConvertToBase64: Image processing error: ${e.message}", e)
            Toast.makeText(this, "Image processing error: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Calculates the `inSampleSize` value for `BitmapFactory.Options` to efficiently scale down an image.
     * This prevents loading large images into memory that are larger than needed for display.
     * @param uri The URI of the image.
     * @param reqWidth The required width for the scaled image.
     * @param reqHeight The required height for the scaled image.
     * @return The calculated inSampleSize.
     */
    private fun calculateInSampleSize(uri: Uri, reqWidth: Int, reqHeight: Int): Int {
        Log.d(TAG, "calculateInSampleSize: Calculating inSampleSize for reqWidth=$reqWidth, reqHeight=$reqHeight")
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // Decode only image bounds, not the full bitmap
        }
        // Use a 'use' block to ensure the InputStream is closed automatically
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options) // Decode stream to get image dimensions
        }

        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1 // Initial inSampleSize value

        // Only calculate inSampleSize if image dimensions are larger than required
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        Log.d(TAG, "calculateInSampleSize: Calculated inSampleSize: $inSampleSize (Original: ${width}x${height})")
        return inSampleSize
    }

    /**
     * Saves the expense data to Firestore.
     * This is a suspend function to allow for asynchronous Firestore operations.
     */
    private suspend fun saveExpenseToFirestore(
        name: String,
        categoryName: String,
        categoryId: String?, // The ID of the category document
        date: String,
        amount: Double,
        description: String,
        base64Image: String?, // The Base64 string of the image
        userId: String
    ) {
        Log.d(TAG, "saveExpenseToFirestore: Preparing data for Firestore save for user: $userId")
        // Create a HashMap to store expense data
        val expenseData = hashMapOf(
            "name" to name,
            "category" to categoryName,
            "categoryId" to categoryId, // Store the category document ID
            "date" to date,
            "amount" to amount,
            "description" to description,
            "imageUrl" to base64Image, // Store Base64 string (renamed to imageUrl for consistency)
            "userId" to userId
        )
        Log.d(TAG, "saveExpenseToFirestore: Expense data payload: $expenseData")

        // Save the expense data to Firestore under the user's expenses subcollection
        firestore.collection("users").document(userId) // Navigate to the user's document
            .collection("expenses") // Access the 'expenses' subcollection
            .add(expenseData) // Add a new document with the generated ID
            .await() // Await the completion of the Firestore operation (throws Exception on failure)
        Log.d(TAG, "saveExpenseToFirestore: Expense document added to Firestore.")
    }

    /**
     * Sets up the item selected listener for the bottom navigation view.
     * This method handles fragment transactions based on the selected menu item.
     */
    private fun setupBottomNav() {
        Log.d(TAG, "setupBottomNav: Setting up bottom navigation.")
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    Log.d(TAG, "BottomNav: Logout selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    Log.d(TAG, "BottomNav: Menu selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    Log.d(TAG, "BottomNav: BudgetingGuides selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    Log.d(TAG, "BottomNav: Awards selected.")
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> {
                    Log.d(TAG, "BottomNav: Unknown item selected (ID: ${item.itemId}).")
                    false
                }
            }
        }
        Log.d(TAG, "setupBottomNav: Bottom navigation listener set.")
    }

    // --- Camera Related Functions ---

    /**
     * Checks for camera permission and requests it if not granted.
     * If permission is granted, dispatches the take picture intent.
     */
    private fun checkCameraPermission() {
        Log.d(TAG, "checkCameraPermission: Checking CAMERA permission.")
        when {
            // Check if permission is already granted
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "checkCameraPermission: CAMERA permission already granted. Dispatching take picture intent.")
                dispatchTakePictureIntent()
            }
            // Check if we should show a rationale for why the permission is needed
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                Log.d(TAG, "checkCameraPermission: Showing camera permission rationale to user.")
                Toast.makeText(this, "Camera permission is required to take photos of receipts.", Toast.LENGTH_LONG).show()
                // Optionally, you could show a dialog here explaining why.
                // For simplicity, requesting permission immediately after rationale.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST
                )
            }
            // Request the permission if it hasn't been granted or denied with "never ask again"
            else -> {
                Log.d(TAG, "checkCameraPermission: Requesting CAMERA permission.")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST
                )
            }
        }
    }

    /**
     * Handles the result of the permission request.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode, permissions=${permissions.joinToString()}, grantResults=${grantResults.joinToString()}")
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: CAMERA permission granted.")
                    dispatchTakePictureIntent() // Permission granted, proceed to take picture
                } else {
                    Log.w(TAG, "onRequestPermissionsResult: CAMERA permission denied.")
                    Toast.makeText(this, "Camera permission denied. Cannot take photo.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Dispatches an intent to capture a picture using the device's camera application.
     * A temporary file is created to store the full-resolution image.
     */
    private fun dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent: Preparing camera intent.")
        // Create ContentValues for the image file metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "expense_${System.currentTimeMillis()}.jpg") // Unique file name
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (Q) and above, use RELATIVE_PATH for scoped storage
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                Log.d(TAG, "dispatchTakePictureIntent: Using RELATIVE_PATH for Android Q+.")
            }
        }

        // Insert a new media item into the MediaStore and get its content URI
        cameraImageUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        Log.d(TAG, "dispatchTakePictureIntent: Temporary image URI created: $cameraImageUri")

        // Create an intent to start the camera application
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            cameraImageUri?.let { uri ->
                // Pass the URI to the camera app so it saves the image to this location
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                Log.d(TAG, "dispatchTakePictureIntent: Starting camera activity for result.")
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            } ?: run {
                Log.e(TAG, "dispatchTakePictureIntent: cameraImageUri is null, cannot start camera.")
                Toast.makeText(this, "Error creating image file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sets up the DatePickerDialog for the date input field.
     */
    private fun setupDatePicker() {
        val calendar = Calendar.getInstance() // Get current date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create and show the DatePickerDialog
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Format the selected date to "YYYY-MM-DD" format
            // Added Locale.US for consistent formatting across different device locales
            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            binding.DATEInput.setText(formattedDate) // Set the formatted date to the EditText
            Log.d(TAG, "setupDatePicker: Date selected: $formattedDate")
        }, year, month, day)

        datePickerDialog.show() // Display the date picker dialog
        Log.d(TAG, "setupDatePicker: DatePickerDialog shown.")
    }
}