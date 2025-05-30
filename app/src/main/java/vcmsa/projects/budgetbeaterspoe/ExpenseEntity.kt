package vcmsa.projects.budgetbeaterspoe

data class ExpenseEntity(
    val id: String = "",
    val name: String = "",
    val category: String = "",  // Changed from categoryId to category name
    val amount: Double = 0.0,
    val date: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val description: String? = null  // Added
)