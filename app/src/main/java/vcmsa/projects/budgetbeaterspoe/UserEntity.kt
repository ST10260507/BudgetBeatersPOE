package vcmsa.projects.budgetbeaterspoe

// Import Room annotations for defining database entities and primary keys
import androidx.room.Entity
import androidx.room.PrimaryKey

// Define a Room entity with the table name "users"
@Entity(tableName = "users")
data class UserEntity(
    // Primary key for the entity; will auto-increment
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // User's unique username
    val username: String,

    // User's email address
    val email: String,

    // User's password (should be securely hashed in real-world apps)
    val password: String
)