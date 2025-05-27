package vcmsa.projects.budgetbeaterspoe

data class ExpenseEntity(
    val id: String = "",              // Firestore document ID
    val name: String = "",            // Name or title of the expense
    val categoryId: String = "",      // Reference to CategoryEntity.id
    val amount: Double = 0.0,
    val date: String = "",            // ISO 8601 or timestamp
    val userId: String = "",          // Reference to UserEntity.id
    val imageUrl: String = ""         // URL to image in Firebase Storage
)

