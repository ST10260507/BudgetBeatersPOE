package vcmsa.projects.budgetbeaterspoe

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Define the database and its entities
@Database(
    entities = [UserEntity::class, CategoryEntity::class, ExpenseEntity::class,
        SharedUserEntity::class ],  // List of entities that the database will handle
    version = 4  // Database version (used for migrations)
)
abstract class AppDatabase : RoomDatabase() {
    // Abstract methods for accessing DAOs (Data Access Objects)
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun sharedUserDao(): SharedUserDao

    companion object {
        // Volatile ensures visibility of the INSTANCE variable across threads
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Function to get or create the database instance
        fun getDatabase(context: Context): AppDatabase {
            // Return the existing instance or create a new one if it doesn't exist
            return INSTANCE ?: synchronized(this) {
                // Build the database using Room's database builder
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Context to access the application resources
                    AppDatabase::class.java,    // Database class type
                    "budget_beaters_db"         // Name of the database file
                ).fallbackToDestructiveMigration() // Fallback to destructive migration in case of version mismatch
                    .build()
                // Store the instance for future use
                INSTANCE = instance
                instance
            }
        }
    }
}
