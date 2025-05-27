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

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.LoginBtn.setOnClickListener {
            val emailOrUsername = binding.LoginNameInput.text.toString().trim()
            val password = binding.PasswordInput.text.toString().trim()

            if (validateInput(emailOrUsername, password)) {
                // Firebase Authentication requires email,
                // but if you want to login by username, you'll need to fetch email from Firestore first
                // Here we'll assume the user inputs an email (adjust if you want username login)
                authenticateUserByEmail(emailOrUsername, password)
            }
        }

        binding.ForgotPasswordBtn.setOnClickListener {
            startActivity(Intent(this, ForgotPassActivity::class.java))
        }

        binding.RegisterBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.LoginNameInput.error = "Email or Username required"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.PasswordInput.error = "Password required"
            isValid = false
        }

        return isValid
    }

    private fun authenticateUserByEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful, get the logged-in user
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Fetch user data from Firestore (optional but recommended)
                        firestore.collection("users")
                            .document(firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val userMap = document.data
                                    val username = userMap?.get("username") as? String ?: email
                                    handleSuccessfulLogin(username)
                                } else {
                                    // User document does not exist, fallback to email as username
                                    handleSuccessfulLogin(email)
                                }
                            }
                            .addOnFailureListener {
                                // Firestore fetch failed, still proceed with email as username
                                handleSuccessfulLogin(email)
                            }
                    }
                } else {
                    // Login failed
                    showLoginError()
                }
            }
    }

    private fun handleSuccessfulLogin(username: String) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("logged_in_user", username)
            apply()
        }

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }

    private fun showLoginError() {
        binding.PasswordInput.error = "Invalid credentials"
        Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show()
    }
}
