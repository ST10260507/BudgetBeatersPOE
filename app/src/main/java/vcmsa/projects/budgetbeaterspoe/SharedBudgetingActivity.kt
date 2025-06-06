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
import com.google.firebase.firestore.DocumentReference

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
        loadExistingSharedUsers() // This will now load from the subcollection

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

        // --- CHANGE START ---
        // Query the subcollection for the current user
        firestore.collection("users").document(uid).collection("sharedBudgetingUsers")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val sharedUsers = querySnapshot.documents.map { doc ->
                    // Make sure this maps correctly to your SharedUserEntity if you changed it
                    SharedUserEntity(
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
                } else {
                    // Clear the input fields if no existing shared users are found
                    memberCountInput.text.clear()
                    membersContainer.removeAllViews()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load shared users: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        // --- CHANGE END ---
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
        membersContainer.removeAllViews() // Clear existing views

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

            val membersToSave = mutableListOf<SharedUserEntity>() // Use SharedUserEntity
            for (i in 0 until membersContainer.childCount step 2) {
                val name = (membersContainer.getChildAt(i) as EditText).text.toString().trim()
                val email = (membersContainer.getChildAt(i + 1) as EditText).text.toString().trim()

                if (name.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields for member ${i / 2 + 1}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Invalid email format for member ${i / 2 + 1}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                membersToSave.add(SharedUserEntity(name, email)) // Create SharedUserEntity object
                finish()
            }

            // --- CHANGE START ---
            val userSharedBudgetsCollection = firestore.collection("users").document(uid).collection("sharedBudgetingUsers")

            // Delete old shared users and add new ones in a batch
            userSharedBudgetsCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = firestore.batch()

                    // Delete existing shared users for this owner
                    for (doc in querySnapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    // Add new shared users
                    membersToSave.forEach { member ->
                        val newDocRef = userSharedBudgetsCollection.document() // Get a new document reference
                        batch.set(newDocRef, member) // Use the SharedUserEntity directly
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Successfully shared with ${membersToSave.size} members!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save shared users: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update shared users: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            // --- CHANGE END ---
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupBottomNav() {
        // Find the BottomNavigationView using its ID from the layout
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Set the listener for item selections
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    // Navigate to LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Menu -> {
                    // Navigate to Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.BudgetingGuides -> {
                    // Navigate to BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Awards -> {
                    // Navigate to AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                else -> false // Item not recognized
            }
        }

        // Optional: Set the default selected item when the activity starts.
        // If this activity is primarily for shared budgeting, you might not have a default fragment for it
        // and rather have the main content (your current layout) as the default.
        // If you were going to add a SharedBudgetingFragment to the bottom nav, you'd select that here.
        // For now, it's likely that the main activity layout is the "screen" for Shared Budgeting.
        // If the bottom nav is shared across multiple activities, this might not be relevant here.
    }
}