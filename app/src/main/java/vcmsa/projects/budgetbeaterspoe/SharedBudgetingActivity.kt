package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log // Import for logging
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
import com.google.firebase.firestore.DocumentReference // This import might not be strictly necessary if not directly used for DocumentReference objects outside batch operations.

/**
 * Activity for managing shared budgeting members.
 * Allows the current user to add or modify a list of other users with whom they share a budget.
 * The shared users are stored in a Firestore subcollection specific to the current user.
 */
class SharedBudgetingActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "SharedBudgetingActivity"

    // Container for dynamically added member input fields
    private lateinit var membersContainer: LinearLayout

    // EditText for entering the number of shared budget members
    private lateinit var memberCountInput: EditText

    // Button to submit the shared budget member information
    private lateinit var submitButton: Button

    // Firebase Authentication instance
    private val auth = FirebaseAuth.getInstance()

    // Firestore instance for database operations
    private val firestore = FirebaseFirestore.getInstance()

    // Convenience getter for the current authenticated user
    private val currentUser get() = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Enable edge-to-edge display for a full-screen experience
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        setContentView(R.layout.activity_shared_budgeting) // Set the layout for this activity
        Log.d(TAG, "onCreate: Layout set to activity_shared_budgeting.")

        // Initialize views from the layout
        membersContainer = findViewById(R.id.membersContainer)
        memberCountInput = findViewById(R.id.memberCountInput)
        submitButton = findViewById(R.id.submitButton)
        Log.d(TAG, "onCreate: Views initialized.")

        // Set up listeners and initial data loading
        setupNumberInputListener() // Listener for member count input changes
        setupSubmitButton() // Listener for the submit button
        loadExistingSharedUsers() // Load any previously saved shared users
        Log.d(TAG, "onCreate: Listeners and initial data loading initiated.")

        // Set up window insets listener for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set.")

        // Set up the bottom navigation bar
        setupBottomNav()
        Log.d(TAG, "onCreate: Bottom navigation setup complete.")
    }

    /**
     * Loads existing shared budget users from the current user's Firestore subcollection
     * and populates the input fields with their data.
     */
    private fun loadExistingSharedUsers() {
        val uid = currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "loadExistingSharedUsers: User not logged in. Cannot load shared users.") // Log warning
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            finish() // Finish activity if user is not authenticated
            return
        }
        Log.d(TAG, "loadExistingSharedUsers: Attempting to load shared users for UID: $uid")

        // Query the 'sharedBudgetingUsers' subcollection under the current user's document
        firestore.collection("users").document(uid).collection("sharedBudgetingUsers")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(TAG, "loadExistingSharedUsers: Successfully fetched shared users. Found ${querySnapshot.size()} documents.")
                val sharedUsers = querySnapshot.documents.map { doc ->
                    // Map each Firestore document to a SharedUserEntity object
                    SharedUserEntity(
                        sharedUserName = doc.getString("sharedUserName") ?: "",
                        sharedUserEmail = doc.getString("sharedUserEmail") ?: ""
                    ).also {
                        Log.d(TAG, "loadExistingSharedUsers: Mapped shared user: ${it.sharedUserName} (${it.sharedUserEmail})")
                    }
                }

                if (sharedUsers.isNotEmpty()) {
                    Log.d(TAG, "loadExistingSharedUsers: Populating fields with ${sharedUsers.size} existing shared users.")
                    // Set the count input to the number of loaded users
                    memberCountInput.setText(sharedUsers.size.toString())
                    membersContainer.removeAllViews() // Clear existing dynamically added views

                    // Add input fields and pre-fill them with loaded user data
                    sharedUsers.forEachIndexed { index, user ->
                        addMemberInputFields(index + 1, user.sharedUserName, user.sharedUserEmail)
                    }
                } else {
                    Log.d(TAG, "loadExistingSharedUsers: No existing shared users found for UID: $uid.")
                    // Clear the input fields if no existing shared users are found
                    memberCountInput.text.clear()
                    membersContainer.removeAllViews()
                }
            }
            .addOnFailureListener { e ->
                // Log and show error if loading shared users fails
                Log.e(TAG, "loadExistingSharedUsers: Failed to load shared users: ${e.message}", e)
                Toast.makeText(this, "Failed to load shared users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Sets up a TextWatcher for the `memberCountInput` to dynamically update
     * the number of member input fields.
     */
    private fun setupNumberInputListener() {
        Log.d(TAG, "setupNumberInputListener: Setting up TextWatcher for memberCountInput.")
        memberCountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* No-op */ }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { /* No-op */ }
            override fun afterTextChanged(s: Editable?) {
                Log.d(TAG, "afterTextChanged: Member count input changed to: ${s.toString()}")
                updateMemberFields() // Call updateMemberFields when text changes
            }
        })
    }

    /**
     * Updates the number of dynamically added input fields for members based on the
     * value in `memberCountInput`.
     */
    private fun updateMemberFields() {
        val memberCount = memberCountInput.text.toString().toIntOrNull() ?: 0
        Log.d(TAG, "updateMemberFields: Updating fields for $memberCount members.")
        membersContainer.removeAllViews() // Clear all existing dynamically added views

        if (memberCount > 0) {
            for (i in 1..memberCount) {
                addMemberInputFields(i) // Add new input fields for each member
            }
        }
    }

    /**
     * Dynamically adds a pair of EditText fields (for name and email) to the `membersContainer`.
     * Optional parameters allow pre-filling the fields (useful when loading existing data).
     * @param memberNumber The sequential number of the member (e.g., 1 for "Member 1").
     * @param name The initial name to set for the EditText (default is empty).
     * @param email The initial email to set for the EditText (default is empty).
     */
    private fun addMemberInputFields(memberNumber: Int, name: String = "", email: String = "") {
        Log.d(TAG, "addMemberInputFields: Adding fields for Member $memberNumber (Name: $name, Email: $email)")
        val nameEditText = EditText(this).apply {
            hint = "Member $memberNumber Name"
            setText(name)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.white, theme)) // Use theme-aware getColor
            setHintTextColor(resources.getColor(android.R.color.white, theme))
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() } // Add bottom margin in dp
        }

        val emailEditText = EditText(this).apply {
            hint = "Member $memberNumber Email"
            setText(email)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS // Set input type for email
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.white, theme)) // Use theme-aware getColor
            setHintTextColor(resources.getColor(android.R.color.white, theme))
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24.dpToPx() } // Add bottom margin in dp
        }

        membersContainer.addView(nameEditText) // Add name EditText to the container
        membersContainer.addView(emailEditText) // Add email EditText to the container
    }

    /**
     * Sets up the click listener for the submit button.
     * Validates input, collects shared member data, and saves/updates it in Firestore.
     */
    private fun setupSubmitButton() {
        Log.d(TAG, "setupSubmitButton: Setting up click listener for submitButton.")
        submitButton.setOnClickListener {
            val uid = currentUser?.uid
            if (uid == null) {
                Log.w(TAG, "submitButton: User not authenticated. Cannot save shared users.") // Log warning
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val memberCount = memberCountInput.text.toString().toIntOrNull() ?: 0
            if (memberCount < 1) {
                Log.w(TAG, "submitButton: Member count is less than 1. Validation failed.") // Log warning
                Toast.makeText(this, "Please enter number of members", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val membersToSave = mutableListOf<SharedUserEntity>() // List to hold SharedUserEntity objects
            // Iterate through the dynamically added EditText pairs
            for (i in 0 until membersContainer.childCount step 2) {
                val name = (membersContainer.getChildAt(i) as EditText).text.toString().trim()
                val email = (membersContainer.getChildAt(i + 1) as EditText).text.toString().trim()

                // Validate individual member fields
                if (name.isEmpty() || email.isEmpty()) {
                    Log.w(TAG, "submitButton: Empty name or email for member ${i / 2 + 1}. Validation failed.")
                    Toast.makeText(this, "Please fill all fields for member ${i / 2 + 1}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener // Stop and return if any field is empty
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Log.w(TAG, "submitButton: Invalid email format for member ${i / 2 + 1}: $email. Validation failed.")
                    Toast.makeText(this, "Invalid email format for member ${i / 2 + 1}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener // Stop and return if email format is invalid
                }
                membersToSave.add(SharedUserEntity(name, email)) // Add valid member to the list
            }
            Log.d(TAG, "submitButton: All input fields validated. Preparing to save ${membersToSave.size} members.")

            // Reference to the 'sharedBudgetingUsers' subcollection for the current user
            val userSharedBudgetsCollection = firestore.collection("users").document(uid).collection("sharedBudgetingUsers")

            // First, get existing documents to delete them in a batch
            userSharedBudgetsCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = firestore.batch() // Create a new Firestore batch operation
                    Log.d(TAG, "submitButton: Starting batch operation. Deleting ${querySnapshot.size()} existing shared users.")

                    // Delete existing shared users for this owner to ensure data consistency (replace, not append)
                    for (doc in querySnapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    // Add new shared users to the batch
                    membersToSave.forEach { member ->
                        val newDocRef = userSharedBudgetsCollection.document() // Get a new document reference
                        batch.set(newDocRef, member) // Use the SharedUserEntity object directly (Firestore will convert it)
                        Log.d(TAG, "submitButton: Adding new shared user to batch: ${member.sharedUserName}")
                    }

                    // Commit the batch operation (deletions and additions)
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "submitButton: Batch commit successful. ${membersToSave.size} members saved.")
                            Toast.makeText(
                                this,
                                "Successfully shared with ${membersToSave.size} members!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish() // Finish the activity after successful save
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "submitButton: Batch commit failed: ${e.message}", e)
                            Toast.makeText(this, "Failed to save shared users: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "submitButton: Failed to retrieve existing shared users for update: ${e.message}", e)
                    Toast.makeText(this, "Failed to update shared users: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Extension function to convert DP (Density-independent Pixels) to PX (Pixels).
     * Useful for setting view dimensions programmatically.
     * @receiver The integer DP value.
     * @return The converted integer pixel value.
     */
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    /**
     * Sets up the item selected listener for the bottom navigation view.
     * This method handles fragment transactions based on the selected menu item.
     */
    private fun setupBottomNav() {
        Log.d(TAG, "setupBottomNav: Setting up bottom navigation.") // Log setup initiation
        // Find the BottomNavigationView using its ID from the layout
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Set the listener for item selections
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    Log.d(TAG, "BottomNav: Logout selected.") // Log menu item selection
                    // Navigate to LogoutFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Menu -> {
                    Log.d(TAG, "BottomNav: Menu selected.") // Log menu item selection
                    // Navigate to Menu_NavFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.BudgetingGuides -> {
                    Log.d(TAG, "BottomNav: BudgetingGuides selected.") // Log menu item selection
                    // Navigate to BudgetingGuidesFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                R.id.Awards -> {
                    Log.d(TAG, "BottomNav: Awards selected.") // Log menu item selection
                    // Navigate to AwardsFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true // Indicate item selection was handled
                }
                else -> {
                    Log.d(TAG, "BottomNav: Unknown item selected (ID: ${item.itemId}).") // Log unknown selection
                    false // Item not recognized
                }
            }
        }
        Log.d(TAG, "setupBottomNav: Bottom navigation listener set.") // Log completion of setup
    }
}