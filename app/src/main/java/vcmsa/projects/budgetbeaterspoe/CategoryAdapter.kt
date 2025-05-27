package vcmsa.projects.budgetbeaterspoe

import android.view.LayoutInflater  // Used to inflate views from XML layout files
import android.view.View  // Represents the UI elements
import android.view.ViewGroup  // A container that holds the item views in the RecyclerView
import android.widget.CheckBox  // Represents a checkbox UI element
import androidx.recyclerview.widget.RecyclerView  // RecyclerView is used for displaying large data sets in a list-like format
import vcmsa.projects.budgetbeaterspoe.R  // Imports the resource file for accessing layout and views

// Adapter class for displaying a list of CategoryEntity items in a RecyclerView
class CategoryAdapter(
    private val categories: MutableList<CategoryEntity>,  // List of categories to display in the RecyclerView
    private val onSelectionChanged: (Set<String>) -> Unit  // Callback function to notify when the selection changes
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    // Set to keep track of selected category IDs
    private val selectedIds = mutableSetOf<String>()

    // ViewHolder class that holds references to UI components in each item view
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.categoryCheckBox)  // The checkbox for selecting categories
    }

    // This method is called when the RecyclerView creates a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the item view layout for each category item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)  // Return the ViewHolder that holds the checkbox reference
    }

    // This method is called to bind data to each ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]  // Get the category at the current position
        holder.checkBox.text = category.categoryName  // Set the category name as the text on the checkbox
        holder.checkBox.isChecked = selectedIds.contains(category.id)  // Set checkbox state based on whether the category is selected

        // Listen for changes to the checkbox state (checked/unchecked)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(category.id)  // Add category ID to the selectedIds set if checked
            } else {
                selectedIds.remove(category.id)  // Remove category ID if unchecked
            }
            onSelectionChanged(selectedIds)  // Notify the caller of the selection change
        }
    }

    // Returns the total number of items in the categories list
    override fun getItemCount() = categories.size

    // Method to update the list of categories and notify the adapter of changes
    fun updateCategories(newCategories: List<CategoryEntity>) {
        categories.clear()  // Clear the existing categories list
        categories.addAll(newCategories)  // Add the new categories to the list
        notifyDataSetChanged()  // Notify the adapter that the data has changed and the view needs to be updated
    }
}
