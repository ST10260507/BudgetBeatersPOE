package vcmsa.projects.budgetbeaterspoe

data class UserEntity(
    val id: String = "",              // Firebase UID or custom ID
    val username: String = "",
    val email: String = "",
    val password: String = ""
)
