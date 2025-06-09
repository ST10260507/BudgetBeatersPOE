package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit // For SharedPreferences edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityLoginBinding

/**
 * Activity for user login. Handles authentication with Firebase Auth
 * and fetches user data from Firestore upon successful login.
 */
class LoginActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "LoginActivity"

    // View binding instance for accessing layout elements
    private lateinit var binding: ActivityLoginBinding

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    // Firestore database instance
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Inflate the layout using view binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        // Enable edge-to-edge display for a more immersive experience
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Apply window insets for system bars to prevent content from going under them
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Set padding to the main view based on system bar insets
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "onCreate: FirebaseAuth initialized.")

        // Set click listener for the "Login" button
        binding.LoginBtn.setOnClickListener {
            Log.d(TAG, "LoginBtn clicked.") // Log button click

            // Get email and password input, trimming whitespace
            val email = binding.LoginNameInput.text.toString().trim()
            val password = binding.PasswordInput.text.toString().trim()
            Log.d(TAG, "LoginBtn: Attempting login with email: $email")

            // Validate inputs before proceeding with authentication
            if (validateInput(email, password)) {
                // If inputs are valid, attempt to authenticate the user
                authenticateUser(email, password)
            } else {
                Log.w(TAG, "LoginBtn: Input validation failed for email: $email")
            }
        }

        // Set click listener for the "Forgot Password" button
        binding.ForgotPasswordBtn.setOnClickListener {
            Log.d(TAG, "ForgotPasswordBtn clicked: Navigating to ForgotPassActivity.") // Log button click
            startActivity(Intent(this, ForgotPassActivity::class.java))
        }

        // Set click listener for the "Register" button
        binding.RegisterBtn.setOnClickListener {
            Log.d(TAG, "RegisterBtn clicked: Navigating to RegisterActivity.") // Log button click
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Validates the provided email and password inputs.
     * Displays an error message for empty fields.
     * @param email The email string to validate.
     * @param password The password string to validate.
     * @return True if both email and password are not empty, false otherwise.
     */
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.LoginNameInput.error = "Email required" // Set error on email input field
            isValid = false
            Log.w(TAG, "validateInput: Email field is empty.") // Log validation failure
        } else {
            binding.LoginNameInput.error = null // Clear error if input is valid
        }

        if (password.isEmpty()) {
            binding.PasswordInput.error = "Password required" // Set error on password input field
            isValid = false
            Log.w(TAG, "validateInput: Password field is empty.") // Log validation failure
        } else {
            binding.PasswordInput.error = null // Clear error if input is valid
        }

        return isValid
    }

    /**
     * Authenticates the user with Firebase using the provided email and password.
     * Handles success and failure of the authentication attempt.
     * @param email The email of the user.
     * @param password The password of the user.
     */
    private fun authenticateUser(email: String, password: String) {
        Log.d(TAG, "authenticateUser: Attempting to sign in with email and password.")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Authentication successful
                    Log.d(TAG, "authenticateUser: Sign-in successful for email: $email")
                    handleSuccessfulLogin(email) // Proceed to handle successful login
                } else {
                    // Authentication failed
                    val exceptionMessage = task.exception?.message ?: "Unknown error"
                    Log.e(TAG, "authenticateUser: Sign-in failed for email: $email. Error: $exceptionMessage", task.exception)
                    binding.PasswordInput.text.clear() // Clear password field
                    binding.PasswordInput.error = "Invalid credentials" // Set error on password field
                    Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Handles a successful login by fetching the user's username from Firestore
     * and then proceeding to save user data and navigate to the main menu.
     * @param email The email used for login.
     */
    private fun handleSuccessfulLogin(email: String) {
        val user = auth.currentUser
        user?.let { firebaseUser ->
            Log.d(TAG, "handleSuccessfulLogin: User logged in, fetching username from Firestore for UID: ${firebaseUser.uid}")
            firestore.collection("users").document(firebaseUser.uid).get()
                .addOnSuccessListener { document ->
                    // Attempt to get the username; fallback to email if not found
                    val username = document.getString("username") ?: email
                    Log.d(TAG, "handleSuccessfulLogin: Username fetched: $username")
                    saveUserAndProceed(username)
                }
                .addOnFailureListener { e ->
                    // Log error if fetching username fails, proceed with email as username
                    Log.e(TAG, "handleSuccessfulLogin: Failed to fetch username from Firestore: ${e.message}", e)
                    saveUserAndProceed(email) // Proceed with email as username if Firestore fetch fails
                }
        } ?: run {
            // This case should ideally not happen if task.isSuccessful is true, but as a safeguard
            Log.w(TAG, "handleSuccessfulLogin: Current user is null after successful login.")
            saveUserAndProceed(email)
        }
    }

    /**
     * Saves the logged-in username to SharedPreferences and navigates to the main menu activity.
     * @param username The username to be saved.
     */
    private fun saveUserAndProceed(username: String) {
        // Save the username to SharedPreferences
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit {
            putString("logged_in_user", username)
            apply() // Apply changes asynchronously
        }
        Log.d(TAG, "saveUserAndProceed: Username '$username' saved to SharedPreferences.")

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
        // Navigate to the MenuActivity
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent)
        Log.d(TAG, "saveUserAndProceed: Navigating to MenuActivity.")
        finish() // Close LoginActivity to prevent going back to it
    }
}