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
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater) // Inflate the layout for the login activity
        setContentView(binding.root)
        enableEdgeToEdge() // Enable edge-to-edge layout for the app

        // Set a listener for applying window insets (system bars like status and navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // Get system bars insets
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Apply padding to the view
            insets
        }

        // Set onClickListener for login button
        binding.LoginBtn.setOnClickListener {
            val username = binding.LoginNameInput.text.toString().trim() // Get the username input
            val password = binding.PasswordInput.text.toString().trim() // Get the password input

            // Validate input fields and proceed to authentication if valid
            if (validateInput(username, password)) {
                authenticateUser(username, password)
            }
        }

        // Set onClickListener for forgot password button
        binding.ForgotPasswordBtn.setOnClickListener {
            // Navigate to ForgotPassActivity for password reset
            startActivity(Intent(this, ForgotPassActivity::class.java))
        }

        // Set onClickListener for register button
        binding.RegisterBtn.setOnClickListener {
            // Navigate to RegisterActivity for user registration
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Function to validate user input for login
    private fun validateInput(username: String, password: String): Boolean {
        var isValid = true

        // Check if username is empty
        if (username.isEmpty()) {
            binding.LoginNameInput.error = "Username required" // Show error if empty
            isValid = false
        }

        // Check if password is empty
        if (password.isEmpty()) {
            binding.PasswordInput.error = "Password required" // Show error if empty
            isValid = false
        }

        return isValid // Return whether input is valid or not
    }

    // Function to authenticate the user with the provided username and password
    private fun authenticateUser(username: String, password: String) {
        lifecycleScope.launch {
            try {
                // Access the database to fetch the user by username or email
                val database = AppDatabase.getDatabase(applicationContext)
                val user = database.userDao().getUserByUsernameOrEmail(username, username)

                runOnUiThread {
                    if (user != null && user.password == password) {
                        handleSuccessfulLogin(user) // Handle successful login
                    } else {
                        showLoginError() // Show login error for invalid credentials
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    // Show error message if any exception occurs during the authentication process
                    Toast.makeText(
                        this@LoginActivity,
                        "Login error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Function to handle successful login
    private fun handleSuccessfulLogin(user: UserEntity) {
        // Save the logged-in user's username in shared preferences
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("logged_in_user", user.username)
            apply() // Apply changes to shared preferences
        }

        // Show a success message
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

        // Start the MenuActivity and finish the login activity
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }

    // Function to show an error message for invalid login credentials
    private fun showLoginError() {
        // Set error on the password input field
        binding.PasswordInput.error = "Invalid credentials"

        // Show a toast message indicating wrong credentials
        Toast.makeText(
            this@LoginActivity,
            "Wrong username or password",
            Toast.LENGTH_SHORT
        ).show()
    }
}
