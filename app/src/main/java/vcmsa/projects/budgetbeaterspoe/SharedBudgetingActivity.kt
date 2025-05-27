package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SharedBudgetingActivity : AppCompatActivity() {
    private lateinit var membersContainer: LinearLayout
    private lateinit var memberCountInput: EditText
    private lateinit var submitButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser get() = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shared_budgeting)

        membersContainer = findViewById(R.id.membersContainer)
        memberCountInput = findViewById(R.id.memberCountInput)
        submitButton = findViewById(R.id.submitButton)

        setupNumberInputListener()
        setupSubmitButton()
        loadExistingSharedUsers()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNav()
    }

    private fun loadExistingSharedUsers() {
        val uid = currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        firestore.collection("sharedUsers")
            .whereEqualTo("ownerUserId", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val sharedUsers = querySnapshot.documents.map { doc ->
                    SharedUser(
                        id = doc.id,
                        ownerUserId = doc.getString("ownerUserId") ?: "",
                        sharedUserName = doc.getString("sharedUserName") ?: "",
                        sharedUserEmail = doc.getString("sharedUserEmail") ?: ""
                    )
                }

                if (sharedUsers.isNotEmpty()) {
                    memberCountInput.setText(sharedUsers.size.toString())
                    membersContainer.removeAllViews()
                    sharedUsers.forEachIndexed { index, user ->
                        addMemberInputFields(index + 1, user.sharedUserName, user.sharedUserEmail)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load shared users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNumberInputListener() {
        memberCountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateMemberFields()
            }
        })
    }

    private fun updateMemberFields() {
        val memberCount = memberCountInput.text.toString().toIntOrNull() ?: 0
        membersContainer.removeAllViews()

        if (memberCount > 0) {
            for (i in 1..memberCount) {
                addMemberInputFields(i)
            }
        }
    }

    private fun addMemberInputFields(memberNumber: Int, name: String = "", email: String = "") {
        val nameEditText = EditText(this).apply {
            hint = "Member $memberNumber Name"
            setText(name)
            setTextSize(16f)
            setTextColor(resources.getColor(android.R.color.white))
            setHintTextColor(resources.getColor(android.R.color.white))
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() }
        }

        val emailEditText = EditText(this).apply {
            hint = "Member $memberNumber Email"
            setText(email)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextSize(16f)
            setTextColor(resources.getColor(android.R.color.white))
            setHintTextColor(resources.getColor(android.R.color.white))
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24.dpToPx() }
        }

        membersContainer.addView(nameEditText)
        membersContainer.addView(emailEditText)
    }

    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            val uid = currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val memberCount = memberCountInput.text.toString().toIntOrNull() ?: 0
            if (memberCount < 1) {
                Toast.makeText(this, "Please enter number of members", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val members = mutableListOf<Map<String, Any>>()
            for (i in 0 until membersContainer.childCount step 2) {
                val name = (membersContainer.getChildAt(i) as EditText).text.toString().trim()
                val email = (membersContainer.getChildAt(i + 1) as EditText).text.toString().trim()

                if (name.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                members.add(
                    mapOf(
                        "ownerUserId" to uid,
                        "sharedUserName" to name,
                        "sharedUserEmail" to email
                    )
                )
            }

            // Delete old shared users and add new ones
            firestore.collection("sharedUsers")
                .whereEqualTo("ownerUserId", uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = firestore.batch()

                    // Delete existing shared users
                    for (doc in querySnapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    // Add new shared users
                    members.forEach { member ->
                        val newDoc = firestore.collection("sharedUsers").document()
                        batch.set(newDoc, member)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Successfully shared with ${members.size} members!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to save shared users", Toast.LENGTH_SHORT)
                                .show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update shared users", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    // Data class to represent shared user data (optional)
    data class SharedUser(
        val id: String = "",
        val ownerUserId: String = "",
        val sharedUserName: String = "",
        val sharedUserEmail: String = ""
    )
}
