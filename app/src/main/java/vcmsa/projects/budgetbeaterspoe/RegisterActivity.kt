package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

// RegisterActivity is the activity where users can register for the app
class RegisterActivity : AppCompatActivity() {
    // Binding to access the views from the XML layout
    private lateinit var binding: ActivityRegisterBinding

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge() // Enable edge-to-edge display

        // Set up the listener for window insets to adjust padding for system bars (e.g., status bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        // Set click listener for the Sign Up button
        binding.SignUpBtn.setOnClickListener {
            // Get user input from the registration fields
            val name = binding.CreateNameInput.text.toString().trim()
            val email = binding.CreateEmailInput.text.toString().trim()
            val password = binding.CreatePasswordInput.text.toString().trim()
            val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

            // Validate input before attempting to register the user
            if (validateInput(name, email, password, confirmPassword)) {
                // Perform registration in a background coroutine
                lifecycleScope.launch {
                    try {
                        val database = AppDatabase.getDatabase(applicationContext) // Get database instance
                        // Check if a user already exists with the same username or email
                        val existingUser = database.userDao().getUserByUsernameOrEmail(name, email)

                        if (existingUser != null) {
                            // If user exists, show error messages for existing username or email
                            runOnUiThread {
                                if (existingUser.username == name) {
                                    binding.CreateNameInput.error = "Username already exists"
                                }
                                if (existingUser.email == email) {
                                    binding.CreateEmailInput.error = "Email already registered"
                                }
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Registration failed: User exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // If no existing user, insert the new user into the database
                            database.userDao().insertUser(
                                UserEntity(
                                    username = name,
                                    email = email,
                                    password = password
                                )
                            )

                            // Fetch the newly inserted user
                            val newUser = database.userDao().getUserByUsername(name)

                            runOnUiThread {
                                // Show success message
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Registration successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Store the logged-in user in shared preferences
                                val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("logged_in_user", newUser?.username ?: "")
                                    apply()
                                }

                                // Navigate to the MenuActivity after successful registration
                                startActivity(Intent(this@RegisterActivity, MenuActivity::class.java))
                                finish() // Close the current activity
                            }
                        }
                    } catch (e: Exception) {
                        // Handle any exceptions that occur during registration
                        runOnUiThread {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Registration failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // Function to validate user input before registration
    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        // Validate name (it cannot be empty)
        if (name.isEmpty()) {
            binding.CreateNameInput.error = "Name required"
            isValid = false
        }

        // Validate email (it cannot be empty and must be in valid format)
        if (email.isEmpty()) {
            binding.CreateEmailInput.error = "Email required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.CreateEmailInput.error = "Invalid email format"
            isValid = false
        }

        // Validate password (it cannot be empty and must be at least 6 characters)
        if (password.isEmpty()) {
            binding.CreatePasswordInput.error = "Password required"
            isValid = false
        } else if (password.length < 6) {
            binding.CreatePasswordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        // Validate confirm password (it must match the password)
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.error = "Confirm password required"
            isValid = false
        } else if (password != confirmPassword) {
            binding.confirmPasswordInput.error = "Passwords don't match"
            isValid = false
        }

        return isValid // Return whether the input is valid
    }
}
