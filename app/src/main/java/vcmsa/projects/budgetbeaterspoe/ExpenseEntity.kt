package vcmsa.projects.budgetbeaterspoe

data class ExpenseEntity(
    val id: String = "",
    val name: String = "",
    val category: String = "",        // Still stores the category name
    val categoryId: String = "",      // NEW: Stores the category document ID
    val amount: Double = 0.0,
    val date: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val description: String? = null
)