package vcmsa.projects.budgetbeaterspoe

// Required Android and RecyclerView imports
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter class for displaying a simple list of categories in a RecyclerView
class SimpleCategoryAdapter(private val categories: List<CategoryEntity>) :
    RecyclerView.Adapter<SimpleCategoryAdapter.ViewHolder>() {

    // ViewHolder class holds reference to the UI elements in each list item
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // TextView that will display the category name
        val categoryName: TextView = itemView.findViewById(R.id.categoryNameText)
    }

    // Called when RecyclerView needs a new ViewHolder (i.e., a new item view)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for a single category item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_category, parent, false)
        return ViewHolder(view)
    }

    // Called to bind data to a ViewHolder at a given position
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the category at the current position
        val category = categories[position]
        // Set the category name text in the TextView
        holder.categoryName.text = category.categoryName
    }

    // Returns the total number of category items
    override fun getItemCount() = categories.size
}
