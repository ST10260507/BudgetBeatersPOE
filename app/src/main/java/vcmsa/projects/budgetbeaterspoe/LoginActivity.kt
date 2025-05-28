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
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityLoginBinding
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding.LoginBtn.setOnClickListener {
            // CHANGED: Use email instead of username
            val email = binding.LoginNameInput.text.toString().trim()
            val password = binding.PasswordInput.text.toString().trim()

            if (validateInput(email, password)) {
                // CHANGED: Directly authenticate with email
                authenticateUser(email, password)
            }
        }

        binding.ForgotPasswordBtn.setOnClickListener {
            startActivity(Intent(this, ForgotPassActivity::class.java))
        }

        binding.RegisterBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // CHANGED: Validate email instead of username
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.LoginNameInput.error = "Email required"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.PasswordInput.error = "Password required"
            isValid = false
        }

        return isValid
    }

    // CHANGED: Simplified authentication method
    private fun authenticateUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    handleSuccessfulLogin(email)
                } else {
                    // CHANGED: Clear password field and show error
                    binding.PasswordInput.text.clear()
                    binding.PasswordInput.error = "Invalid credentials"
                    Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleSuccessfulLogin(email: String) {
        // CHANGED: Fetch username from Firestore
        val user = auth.currentUser
        user?.let {
            firestore.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username") ?: email
                    saveUserAndProceed(username)
                }
                .addOnFailureListener {
                    saveUserAndProceed(email)
                }
        } ?: saveUserAndProceed(email)
    }

    // Changed this logic
    private fun saveUserAndProceed(username: String) {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit {
            putString("logged_in_user", username)
            apply()
        }

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }
}