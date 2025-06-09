package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log // Import for logging
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

// Fragment that displays budgeting guides with clickable links
class BudgetingGuidesFragment : Fragment() {

    // TAG for logging messages related to this fragment
    private val TAG = "BudgetingGuidesFragment"

    // Suppressing warning related to missing inflated ID (for compatibility purposes)
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Fragment view is being created.") // Log fragment creation

        // Inflate the fragment's layout
        val view = inflater.inflate(R.layout.fragment_budgeting_guides, container, false)
        Log.d(TAG, "onCreateView: Fragment layout inflated.") // Log layout inflation

        // List of links to be added to TextViews for budgeting guides
        val linkViews = listOf(
            Pair(R.id.LinkOne, "https://bettermoneyhabits.bankofamerica.com/en/saving-budgeting/creating-a-budget"), // Link 1
            Pair(R.id.LinkTwo, "https://www.investopedia.com/financial-edge/1109/6-reasons-why-you-need-a-budget.aspx"), // Link 2
            Pair(R.id.LinkThree, "https://www.youtube.com/watch?v=w_RKtck8XCA"), // Link 3
            Pair(R.id.LinkFour, "https://www.youtube.com/watch?v=Py3rkSwsbyw")  // Link 4
        )
        Log.d(TAG, "onCreateView: Link data initialized.") // Log link data initialization

        // Loop through each link and apply it to the corresponding TextView
        for ((id, url) in linkViews) {
            // Find the TextView by its ID
            val textView = view.findViewById<TextView>(id)
            Log.d(TAG, "onCreateView: Found TextView with ID: $id") // Log TextView found

            // Create a SpannableString to handle the link text
            val spannable = SpannableString(url)
            Log.d(TAG, "onCreateView: Created SpannableString for URL: $url") // Log SpannableString creation

            // Add clickable links to the text (WEB_URLS)
            Linkify.addLinks(spannable, Linkify.WEB_URLS)
            Log.d(TAG, "onCreateView: Links added to SpannableString.") // Log link addition

            // Set the text for the TextView with the SpannableString (makes it clickable)
            textView.text = spannable
            Log.d(TAG, "onCreateView: SpannableString set to TextView.") // Log text setting

            // Change the link text color to white
            textView.setLinkTextColor(Color.WHITE)
            Log.d(TAG, "onCreateView: Link text color set to white.") // Log color change

            // Enable clickable links in the TextView
            textView.movementMethod = LinkMovementMethod.getInstance()
            Log.d(TAG, "onCreateView: Link movement method enabled.") // Log movement method enabled
        }
        Log.d(TAG, "onCreateView: All links processed.") // Log link processing completion

        // Return the view for the fragment
        Log.d(TAG, "onCreateView: Returning fragment view.") // Log view return
        return view
    }
}