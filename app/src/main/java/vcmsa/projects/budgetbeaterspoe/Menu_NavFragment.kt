package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

// Menu_NavFragment: A simple Fragment subclass for displaying a navigation menu.
// This fragment contains buttons that lead to various other activities within the application.
class Menu_NavFragment : Fragment() {

    // TAG for logging messages related to this fragment
    private val TAG = "Menu_NavFragment"

    // onCreateView is called to inflate the fragment's view and set up its initial state.
    // Suppressing warning for MissingInflatedId as views are found programmatically without direct ID checks in the XML.
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Fragment view is being created.") // Log fragment creation

        // Inflate the fragment's layout from fragment_menu__nav.xml
        val view = inflater.inflate(R.layout.fragment_menu__nav, container, false)
        Log.d(TAG, "onCreateView: Fragment layout inflated.") // Log layout inflation

        // Set up click listeners for each navigation button
        // 1. View Pie Chart Button
        view.findViewById<Button>(R.id.viewPieChartBtn).setOnClickListener {
            Log.d(TAG, "viewPieChartBtn clicked: Launching PieChartActivity.") // Log button click
            // Create an Intent to start PieChartActivity
            startActivity(Intent(requireContext(), PieChartActivity::class.java))
        }

        // 2. View All Expenses Button
        view.findViewById<Button>(R.id.viewAllExpensesBtn).setOnClickListener {
            Log.d(TAG, "viewAllExpensesBtn clicked: Launching ViewAllExpensesActivity.") // Log button click
            // Create an Intent to start ViewAllExpensesActivity
            startActivity(Intent(requireContext(), ViewAllExpensesActivity::class.java))
        }

        // 3. View Daily Spending Button
        view.findViewById<Button>(R.id.viewDailySpendingBtn).setOnClickListener {
            Log.d(TAG, "viewDailySpendingBtn clicked: Launching ViewAllSpendingActivity.") // Log button click
            // Create an Intent to start ViewAllSpendingActivity
            startActivity(Intent(requireContext(), ViewAllSpendingActivity::class.java))
        }

        // 4. View Progress Dashboard Button
        view.findViewById<Button>(R.id.viewProgressDashboardBtn).setOnClickListener {
            Log.d(TAG, "viewProgressDashboardBtn clicked: Launching ProgressDashboardActivity.") // Log button click
            // Create an Intent to start ProgressDashboardActivity
            startActivity(Intent(requireContext(), ProgressDashboardActivity::class.java))
        }

        // 5. Shared Budgeting Button
        view.findViewById<Button>(R.id.sharedBudgetingBtn).setOnClickListener {
            Log.d(TAG, "sharedBudgetingBtn clicked: Launching SharedBudgetingActivity.") // Log button click
            // Create an Intent to start SharedBudgetingActivity
            startActivity(Intent(requireContext(), SharedBudgetingActivity::class.java))
        }

        // 6. Categories Button
        view.findViewById<Button>(R.id.categoriesBtn).setOnClickListener {
            Log.d(TAG, "categoriesBtn clicked: Launching CategoriesActivity.") // Log button click
            // Create an Intent to start CategoriesActivity
            startActivity(Intent(requireContext(), CategoriesActivity::class.java))
        }

        Log.d(TAG, "onCreateView: All button listeners set.") // Log completion of listener setup

        // Return the inflated view for the fragment to be displayed
        return view
    }
}