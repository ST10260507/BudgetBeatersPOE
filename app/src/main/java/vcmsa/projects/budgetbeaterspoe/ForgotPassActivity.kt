package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ForgotPassActivity : AppCompatActivity() {

    // Override the onCreate method to set up the activity
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass) // Set the layout for the Forgot Password activity

        // Set a listener for applying window insets (system bars like status and navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // Get system bars insets
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Apply padding to the view
            insets
        }

        // Get references to UI elements (EditText, Buttons, and TextView)
        val emailInput = findViewById<EditText>(R.id.createEmailInput) // Email input field
        val submitBtn = findViewById<Button>(R.id.submitBtn) // Submit button for email
        val infoText = findViewById<TextView>(R.id.alreadyRegisteredtxt) // TextView for showing registration info
        val loginBtn = findViewById<Button>(R.id.LoginBtn) // Button to go back to login activity

        // Set onClickListener for the submit button
        submitBtn.setOnClickListener {
            val email = emailInput.text.toString().trim() // Get the email entered by the user

            // Check if the email field is empty
            if (email.isEmpty()) {
                Toast.makeText(this, "PLEASE ENTER YOUR EMAIL", Toast.LENGTH_SHORT).show() // Show toast if empty
                return@setOnClickListener
            }

            // Access the database to check if a user with the entered email exists
            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                val userDao = db.userDao() // Get the user DAO
                val user = userDao.getUserByEmail(email) // Check if user exists with this email

                runOnUiThread {
                    // If a user is found with the entered email, navigate to the ResetPassword activity
                    if (user != null) {
                        val intent = Intent(this@ForgotPassActivity, ResetPassword::class.java)
                        intent.putExtra("email", email) // Pass email to the ResetPassword activity
                        startActivity(intent)
                    } else {
                        // If no user is found, display an error message
                        infoText.text = "NO USER FOUND WITH THIS EMAIL" // Set error message text
                        infoText.setTextColor(getColor(android.R.color.holo_red_light)) // Set text color to red
                    }
                }
            }
        }

        // Set onClickListener for the login button to navigate back to the login screen
        loginBtn.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java)) // Start the LoginActivity
        }
    }
}
