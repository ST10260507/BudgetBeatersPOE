package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
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

// Activity responsible for resetting the user's password
class ResetPassword : AppCompatActivity() {

    // Suppresses warnings for missing inflated views (related to button clicks)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password) // Set the layout for the activity

        // Initialize UI elements
        val newPasswordInput = findViewById<EditText>(R.id.newPassword)
        val resetBtn = findViewById<Button>(R.id.resetBtn)
        val successMessage = findViewById<TextView>(R.id.successMessage)

        // Retrieve the email passed through the intent
        val email = intent.getStringExtra("email") ?: return

        // Set an onClickListener on the reset button
        resetBtn.setOnClickListener {
            // Get the new password entered by the user
            val newPassword = newPasswordInput.text.toString().trim()

            // Check if the new password is at least 6 characters long
            if (newPassword.length < 6) {
                // Show a Toast message if the password is too short
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the database instance for accessing user data
            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                val userDao = db.userDao() // Access the userDao to interact with the database
                val user = userDao.getUserByEmail(email) // Retrieve the user by email

                // If user exists, update their password
                if (user != null) {
                    val updatedUser = user.copy(password = newPassword) // Copy the user with the new password
                    userDao.updateUser(updatedUser) // Update the user in the database

                    // Update the UI on the main thread
                    runOnUiThread {
                        successMessage.visibility = View.VISIBLE // Show success message
                        Toast.makeText(this@ResetPassword, "Password reset successful", Toast.LENGTH_SHORT).show()

                        // Optional: Navigate to the login screen after a delay
                        resetBtn.postDelayed({
                            val intent = Intent(this@ResetPassword, LoginActivity::class.java) // Create an intent for the login screen
                            startActivity(intent) // Start the login activity
                            finish() // Finish the current activity to remove it from the stack
                        }, 2000) // Delay the navigation for 2 seconds
                    }
                } else {
                    // If the user was not found, show an error message
                    runOnUiThread {
                        Toast.makeText(this@ResetPassword, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
