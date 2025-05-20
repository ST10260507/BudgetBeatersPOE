package vcmsa.projects.budgetbeaterspoe

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Adapter for displaying a list of filtered expenses in a RecyclerView
class FilteredExpenseAdapter(
    private val expenses: List<ExpenseEntity> // List of ExpenseEntity objects passed to the adapter
) : RecyclerView.Adapter<FilteredExpenseAdapter.ViewHolder>() {

    // ViewHolder class to hold the views for each item in the RecyclerView
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView       = itemView.findViewById(R.id.expenseName) // TextView for expense name
        val amount: TextView     = itemView.findViewById(R.id.expenseAmount) // TextView for expense amount
        val date: TextView       = itemView.findViewById(R.id.expenseDate) // TextView for expense date
        val image: ImageView     = itemView.findViewById(R.id.expenseImage) // ImageView for expense image
    }

    // Method to create the ViewHolder and inflate the layout for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false) // Inflate the layout for each expense item
        return ViewHolder(view) // Return a new ViewHolder instance
    }

    // Method to bind data to the views in the ViewHolder for each item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position] // Get the expense data for the current position
        holder.name.text   = expense.name // Set the expense name
        holder.amount.text = "R%.2f".format(expense.amount) // Set the formatted expense amount
        holder.date.text   = expense.date // Set the expense date

        // If the expense has an image path, load and display the image
        if (!expense.imagePath.isNullOrEmpty()) {
            holder.image.visibility = View.VISIBLE // Make the image visible
            Glide.with(holder.itemView.context) // Use Glide to load the image
                .load(Uri.parse(expense.imagePath)) // Parse the image path to a URI
                .into(holder.image) // Set the loaded image into the ImageView
        } else {
            holder.image.visibility = View.GONE // If no image, hide the ImageView
        }
    }

    // Method to get the total number of items in the expenses list
    override fun getItemCount(): Int = expenses.size // Return the size of the expenses list
}
