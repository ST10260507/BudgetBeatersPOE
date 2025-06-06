package vcmsa.projects.budgetbeaterspoe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter class for displaying a list of expenses in a RecyclerView
class ExpenseAdapter(
    private var expenses: List<ExpenseEntity>, // Changed to 'var' to allow re-assignment
    private val onExpenseSelected: (ExpenseEntity) -> Unit // Lambda function to handle item selection
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    // ViewHolder class to hold references to views for each item in the list
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.expenseName) // TextView to display the expense name
        val amount: TextView = itemView.findViewById(R.id.expenseAmount) // TextView to display the expense amount
        val date: TextView = itemView.findViewById(R.id.expenseDate) // TextView to display the expense date
    }

    // Called to create a new ViewHolder object when a new item view is needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the item layout for each expense item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ViewHolder(view) // Return the new ViewHolder with the inflated view
    }

    // Called to bind data to the views in each ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position] // Get the expense at the current position
        holder.name.text = expense.name // Set the name of the expense
        holder.amount.text = "R%.2f".format(expense.amount) // Set the amount of the expense (formatted to 2 decimal places)
        holder.date.text = expense.date // Set the date of the expense

        // Set an onClickListener to handle when the item is selected
        holder.itemView.setOnClickListener {
            onExpenseSelected(expense) // Trigger the callback with the selected expense
        }
    }

    // Returns the total number of items in the list
    override fun getItemCount() = expenses.size

    // NEW METHOD: Allows updating the list of expenses and refreshes the RecyclerView
    fun updateExpenses(newExpenses: List<ExpenseEntity>) {
        this.expenses = newExpenses // Update the internal list
        notifyDataSetChanged() // Notify the adapter that the data set has changed
    }
}