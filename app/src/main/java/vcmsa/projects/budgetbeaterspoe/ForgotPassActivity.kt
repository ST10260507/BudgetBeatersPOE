package vcmsa.projects.budgetbeaterspoe

import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityForgotPassBinding

/**
 * Activity for handling password reset functionality.
 * Allows users to request a password reset email via their registered email address.
 */
class ForgotPassActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "ForgotPassActivity"

    // View binding instance for accessing layout elements
    private lateinit var binding: ActivityForgotPassBinding

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Inflate the layout using view binding
        binding = ActivityForgotPassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Layout inflated and content view set.")

        // Enable edge-to-edge display for a more immersive experience
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Initialize Firebase Authentication instance
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "onCreate: FirebaseAuth initialized.")

        // Apply window insets for system bars to prevent content from going under them
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Set padding to the main view based on system bar insets
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Set click listener for the "Submit" (password reset) button
        binding.submitBtn.setOnClickListener {
            Log.d(TAG, "submitBtn clicked.") // Log button click

            // Get the email input and trim whitespace
            val email = binding.createEmailInput.text.toString().trim()

            // Validate if the email field is empty
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "submitBtn: Email input is empty.") // Log warning for empty email
                return@setOnClickListener // Exit the listener if email is empty
            }

            Log.d(TAG, "submitBtn: Sending password reset email to: $email")

            // Send password reset email using Firebase Authentication
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Password reset email sent successfully
                        Log.d(TAG, "sendPasswordResetEmail: Password reset email sent successfully to $email.")
                        Toast.makeText(this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show()

                        // Navigate back to LoginActivity after sending the email
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        Log.d(TAG, "sendPasswordResetEmail: Navigating to LoginActivity.")
                        finish() // Close ForgotPassActivity to prevent going back to it
                    } else {
                        // Failed to send password reset email
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        Log.e(TAG, "sendPasswordResetEmail: Failed to send password reset email: $errorMessage", task.exception)
                        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Set click listener for the "Login" button to navigate back to the login screen
        binding.LoginBtn.setOnClickListener {
            Log.d(TAG, "LoginBtn clicked: Navigating back to LoginActivity.") // Log button click
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close ForgotPassActivity
        }
    }
}