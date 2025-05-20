package vcmsa.projects.budgetbeaterspoe

// Import Room annotations for defining database entities and primary keys
import androidx.room.Entity
import androidx.room.PrimaryKey

// Define a Room Entity representing a shared user entry in the "shared_users" table
@Entity(tableName = "shared_users")
data class SharedUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generated unique ID for each shared user record
    val ownerUserId: Int,           // ID of the original user who is sharing their data
    val sharedUserName: String,     // Name of the user with whom data is being shared
    val sharedUserEmail: String     // Email of the user with whom data is being shared
)
