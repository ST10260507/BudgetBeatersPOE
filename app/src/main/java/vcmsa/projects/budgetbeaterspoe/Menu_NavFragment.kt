package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1" // Placeholder constant for argument 1
private const val ARG_PARAM2 = "param2" // Placeholder constant for argument 2

/**
 * A simple [Fragment] subclass.
 * Use the [Menu_NavFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class Menu_NavFragment : Fragment() {

    // onCreateView is called to inflate the fragment's view
    @SuppressLint("MissingInflatedId") // Suppress warning for missing inflated IDs
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment's layout and assign it to 'view'
        val view = inflater.inflate(R.layout.fragment_menu__nav, container, false)

        // Set up button listeners for each button in the layout
        view.findViewById<Button>(R.id.viewPieChartBtn).setOnClickListener {
            // On clicking 'viewPieChartBtn', launch PieChartActivity
            startActivity(Intent(requireContext(), PieChartActivity::class.java))
        }

        view.findViewById<Button>(R.id.viewAllExpensesBtn).setOnClickListener {
            // On clicking 'viewAllExpensesBtn', launch ViewAllExpensesActivity
            startActivity(Intent(requireContext(), ViewAllExpensesActivity::class.java))
        }

        view.findViewById<Button>(R.id.viewDailySpendingBtn).setOnClickListener {
            // On clicking 'viewDailySpendingBtn', launch ViewAllSpendingActivity
            startActivity(Intent(requireContext(), ViewAllSpendingActivity::class.java))
        }

        view.findViewById<Button>(R.id.viewProgressDashboardBtn).setOnClickListener {
            // On clicking 'viewProgressDashboardBtn', launch ProgressDashboardActivity
            startActivity(Intent(requireContext(), ProgressDashboardActivity::class.java))
        }

        view.findViewById<Button>(R.id.sharedBudgetingBtn).setOnClickListener {
            // On clicking 'sharedBudgetingBtn', launch SharedBudgetingActivity
            startActivity(Intent(requireContext(), SharedBudgetingActivity::class.java))
        }

        view.findViewById<Button>(R.id.categoriesBtn).setOnClickListener {
            // On clicking 'categoriesBtn', launch CategoriesActivity
            startActivity(Intent(requireContext(), CategoriesActivity::class.java))
        }

        // Return the view for the fragment to be displayed
        return view
    }
}
