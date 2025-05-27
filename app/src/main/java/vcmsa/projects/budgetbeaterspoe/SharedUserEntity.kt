package vcmsa.projects.budgetbeaterspoe

data class SharedUserEntity(
    val id: String = "",              // Firestore document ID
    val ownerUserId: String = "",     // The original user's ID
    val sharedUserId: String = ""     // The ID of the user it is shared with
)
