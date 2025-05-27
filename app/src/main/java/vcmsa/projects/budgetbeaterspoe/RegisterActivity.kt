package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        binding.SignUpBtn.setOnClickListener {
            val name = binding.CreateNameInput.text.toString().trim()
            val email = binding.CreateEmailInput.text.toString().trim()
            val password = binding.CreatePasswordInput.text.toString().trim()
            val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                registerUser(name, email, password)
            }
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.CreateNameInput.error = "Name required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.CreateEmailInput.error = "Email required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.CreateEmailInput.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.CreatePasswordInput.error = "Password required"
            isValid = false
        } else if (password.length < 6) {
            binding.CreatePasswordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.error = "Confirm password required"
            isValid = false
        } else if (password != confirmPassword) {
            binding.confirmPasswordInput.error = "Passwords don't match"
            isValid = false
        }

        return isValid
    }

    private fun registerUser(name: String, email: String, password: String) {
        // Create user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration success: add additional user data to Firestore
                    val userId = auth.currentUser?.uid ?: ""

                    val userMap = hashMapOf(
                        "username" to name,
                        "email" to email
                    )

                    firestore.collection("users").document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                            // Save username in shared preferences
                            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("logged_in_user", name)
                                apply()
                            }

                            // Go to menu activity
                            startActivity(Intent(this, MenuActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Registration failed
                    val errorMessage = task.exception?.localizedMessage ?: "Registration failed."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
