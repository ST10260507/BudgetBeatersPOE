package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class LogoutFragment : Fragment() {

    @SuppressLint("MissingInflatedId") // Suppress the warning for missing inflated ID
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_logout, container, false)

        // Find the logout confirmation button in the layout
        val logoutButton = view.findViewById<Button>(R.id.logoutConfirmButton)

        // Set a click listener on the logout button
        logoutButton.setOnClickListener {
            // Optional: Perform any additional actions such as clearing shared preferences or Firebase auth

            // Create an intent to start the LoginActivity
            // FLAG_ACTIVITY_NEW_TASK ensures the LoginActivity is started as a new task
            // FLAG_ACTIVITY_CLEAR_TASK clears the existing task stack, so the user cannot navigate back
            val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            // Start the LoginActivity
            startActivity(intent)
        }

        // Return the inflated view for the fragment
        return view
    }
}
