package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityRegisterBinding

/**
 * Activity for user registration. Handles creating new user accounts with Firebase Authentication
 * and storing additional user data (username) in Firebase Firestore.
 */
class RegisterActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "RegisterActivity"

    // View binding instance for accessing layout elements
    private lateinit var binding: ActivityRegisterBinding

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Inflate the layout using ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set the root view for this activity
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        // Enable edge-to-edge display for a full-screen experience
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Set up window insets listener for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "onCreate: FirebaseAuth instance obtained.")

        // Set click listener for the "Sign Up" button
        binding.SignUpBtn.setOnClickListener {
            Log.d(TAG, "SignUpBtn clicked.") // Log button click

            // Retrieve user input from EditText fields
            val name = binding.CreateNameInput.text.toString().trim()
            val email = binding.CreateEmailInput.text.toString().trim()
            val password = binding.CreatePasswordInput.text.toString().trim()
            val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

            // Validate the user input
            if (validateInput(name, email, password, confirmPassword)) {
                Log.d(TAG, "SignUpBtn: Input validation successful. Attempting to register user.")
                // If input is valid, proceed with user registration
                registerUser(name, email, password)
            } else {
                Log.w(TAG, "SignUpBtn: Input validation failed.")
            }
        }
    }

    /**
     * Validates the user input fields (name, email, password, confirm password).
     * Displays error messages if any input is invalid.
     * @return true if all inputs are valid, false otherwise.
     */
    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        Log.d(TAG, "validateInput: Starting input validation.")

        // Validate Name
        if (name.isEmpty()) {
            binding.CreateNameInput.error = "Name required"
            Log.w(TAG, "validateInput: Name is empty.")
            isValid = false
        }

        // Validate Email
        if (email.isEmpty()) {
            binding.CreateEmailInput.error = "Email required"
            Log.w(TAG, "validateInput: Email is empty.")
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.CreateEmailInput.error = "Invalid email format"
            Log.w(TAG, "validateInput: Invalid email format: $email")
            isValid = false
        }

        // Validate Password
        if (password.isEmpty()) {
            binding.CreatePasswordInput.error = "Password required"
            Log.w(TAG, "validateInput: Password is empty.")
            isValid = false
        } else if (password.length < 6) {
            binding.CreatePasswordInput.error = "Password must be at least 6 characters"
            Log.w(TAG, "validateInput: Password is too short (less than 6 characters).")
            isValid = false
        }

        // Validate Confirm Password
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.error = "Confirm password required"
            Log.w(TAG, "validateInput: Confirm password is empty.")
            isValid = false
        } else if (password != confirmPassword) {
            binding.confirmPasswordInput.error = "Passwords don't match"
            Log.w(TAG, "validateInput: Passwords do not match.")
            isValid = false
        }
        Log.d(TAG, "validateInput: Validation complete. isValid = $isValid")
        return isValid
    }

    /**
     * Registers a new user with Firebase Authentication and stores their username in Firestore.
     * @param name The username to register.
     * @param email The email to register.
     * @param password The password for the new user.
     */
    private fun registerUser(name: String, email: String, password: String) {
        Log.d(TAG, "registerUser: Attempting to create user with email: $email")
        // Create user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "registerUser: Firebase Authentication user creation successful.")
                    // Registration success: add additional user data to Firestore
                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        Log.e(TAG, "registerUser: User ID is null after successful authentication.")
                        Toast.makeText(this, "Error: User ID not found after registration.", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val userMap = hashMapOf(
                        "username" to name,
                        "email" to email
                    )
                    Log.d(TAG, "registerUser: Saving user data to Firestore for UID: $userId")

                    // Store user data in Firestore under a 'users' collection
                    firestore.collection("users").document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "registerUser: User data successfully saved to Firestore.")
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                            // Save username in shared preferences for persistent login/display
                            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("logged_in_user", name)
                                apply()
                            }
                            Log.d(TAG, "registerUser: Username saved to SharedPreferences.")

                            // Navigate to the Menu activity
                            startActivity(Intent(this, MenuActivity::class.java))
                            finish() // Finish this activity to prevent going back to registration
                            Log.d(TAG, "registerUser: Navigated to MenuActivity.")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "registerUser: Failed to save user data to Firestore: ${e.message}", e)
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Registration failed with Firebase Authentication
                    val errorMessage = task.exception?.localizedMessage ?: "Registration failed."
                    Log.e(TAG, "registerUser: Firebase Authentication registration failed: $errorMessage", task.exception)
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}