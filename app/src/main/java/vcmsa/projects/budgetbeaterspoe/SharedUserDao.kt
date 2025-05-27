package vcmsa.projects.budgetbeaterspoe

// Import Room annotations for DAO, insertion, and query operations
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Data Access Object (DAO) interface for handling shared user database operations
@Dao
interface SharedUserDao {

    // Inserts a new shared user into the "shared_users" table
    @Insert
    suspend fun insertSharedUser(sharedUser: SharedUserEntity)

    // Retrieves all shared users for a specific owner by their user ID
    @Query("SELECT * FROM shared_users WHERE ownerUserId = :ownerId")
    suspend fun getSharedUsersByOwner(ownerId: Int): List<SharedUserEntity>

    // Deletes all shared users associated with a specific owner user ID
    @Query("DELETE FROM shared_users WHERE ownerUserId = :ownerId")
    suspend fun deleteSharedUsersForOwner(ownerId: Int)
}
