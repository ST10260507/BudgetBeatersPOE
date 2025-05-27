package vcmsa.projects.budgetbeaterspoe

data class CategoryEntity(
    val id: String = "",              // Firestore document ID
    val categoryName: String = "",
    val maxLimit: Int = 0,
    val minLimit: Int = 0,
    val userId: String = ""           // Reference to UserEntity.id
)