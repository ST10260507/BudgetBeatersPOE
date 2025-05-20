package vcmsa.projects.budgetbeaterspoe

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity class for the "expenses" table in the Room database
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Unique ID for each expense (auto-generated)
    val name: String, // Name of the expense (e.g., "Lunch", "Transport")
    val category: String, // Category of the expense (e.g., "Food", "Travel")
    val date: String, // Date when the expense occurred
    val amount: Double, // Amount spent on the expense
    val description: String?, // Optional description of the expense (nullable)
    val imagePath: String? // Optional path to the image related to the expense (nullable)
)
