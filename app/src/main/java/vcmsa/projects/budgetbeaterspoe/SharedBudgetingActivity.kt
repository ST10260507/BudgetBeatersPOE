package vcmsa.projects.budgetbeaterspoe

// Import required Android and lifecycle libraries
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class SharedBudgetingActivity : AppCompatActivity() {
    private lateinit var membersContainer: LinearLayout  // Container for member input fields
    private lateinit var memberCountInput: EditText      // Input field for number of members
    private lateinit var submitButton: Button            // Button to submit members
    private lateinit var database: AppDatabase           // App database instance
    private var currentUserId: Int = -1                  // Stores logged-in user's ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shared_budgeting)

        // Initialize views and database
        database = AppDatabase.getDatabase(this)
        membersContainer = findViewById(R.id.membersContainer)
        memberCountInput = findViewById(R.id.memberCountInput)
        submitButton = findViewById(R.id.submitButton)

        // Setup listeners and data
        setupNumberInputListener()
        setupSubmitButton()
        loadCurrentUser()

        // Handle edge-to-edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNav() // Initialize bottom navigation
    }

    // Loads the currently logged-in user's ID from shared preferences
    private fun loadCurrentUser() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("logged_in_user", "") ?: ""

        lifecycleScope.launch {
            val user = database.userDao().getUserByUsername(username)
            if (user != null) {
                currentUserId = user.id
                loadExistingSharedUsers()
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@SharedBudgetingActivity,
                        "User not logged in!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish() // Close activity if no user is logged in
                }
            }
        }
    }

    // Loads and displays shared users previously saved for the current user
    private fun loadExistingSharedUsers() {
        lifecycleScope.launch {
            val sharedUsers = database.sharedUserDao().getSharedUsersByOwner(currentUserId)
            runOnUiThread {
                if (sharedUsers.isNotEmpty()) {
                    memberCountInput.setText(sharedUsers.size.toString())
                    sharedUsers.forEachIndexed { index, sharedUser ->
                        addMemberInputFields(index + 1, sharedUser.sharedUserName, sharedUser.sharedUserEmail)
                    }
                }
            }
        }
    }

    // Listens for changes in the member count input to dynamically update UI
    private fun setupNumberInputListener() {
        memberCountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateMemberFields()
            }
        })
    }

    // Updates the number of member input fields based on the entered count
    private fun updateMemberFields() {
        val memberCount = memberCountInput.text.toString().toIntOrNull() ?: 0
        membersContainer.removeAllViews()

        if (memberCount > 0) {
            for (i in 1..memberCount) {
                addMemberInputFields(i)
            }
        }
    }

    // Adds input fields for a member's name and email
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
            ).apply {
                bottomMargin = 16.dpToPx()
            }
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
            ).apply {
                bottomMargin = 24.dpToPx()
            }
        }

        membersContainer.addView(nameEditText)
        membersContainer.addView(emailEditText)
    }

    // Handles submission of shared members to the database
    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            if (currentUserId == -1) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val memberCount = memberCountInput.text.toString().toIntOrNull() ?: 0
            if (memberCount < 1) {
                Toast.makeText(this, "Please enter number of members", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val members = mutableListOf<Pair<String, String>>()
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
                members.add(Pair(name, email))
            }

            // Save members to the database using a coroutine
            lifecycleScope.launch {
                // Remove previous shared users
                database.sharedUserDao().deleteSharedUsersForOwner(currentUserId)

                // Insert new shared user records
                members.forEach { (name, email) ->
                    database.sharedUserDao().insertSharedUser(
                        SharedUserEntity(
                            ownerUserId = currentUserId,
                            sharedUserName = name,
                            sharedUserEmail = email
                        )
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@SharedBudgetingActivity,
                        "Successfully shared with ${members.size} members!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Extension function to convert dp values to pixels
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // Sets up the bottom navigation bar and its fragment actions
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
}
