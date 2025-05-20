package vcmsa.projects.budgetbeaterspoe


import androidx.room.Dao  // Room Data Access Object (DAO) annotation to define methods for interacting with the database
import androidx.room.Insert  // Room annotation to define insert operation for a database entity
import androidx.room.OnConflictStrategy  // Room annotation to handle conflict strategies during insertions
import androidx.room.Query  // Room annotation for SQL queries in DAO

@Dao  // Marks this interface as a Data Access Object (DAO) for Room Database operations
interface CategoryDao {

    // This commented-out method is for inserting a category into the database, where conflicts would cause an abort
    // @Insert(onConflict = OnConflictStrategy.ABORT)
    // suspend fun insertCategory(category: CategoryEntity)

    // Query method to delete categories by their IDs
    @Query("DELETE FROM categories WHERE id IN (:categoryIds)")
    suspend fun deleteCategoriesByIds(categoryIds: List<Int>)  // This method deletes categories by their provided IDs

    // This method is for inserting a new category into the database, aborting on conflict (e.g., duplicate IDs)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity)

    // Query method to retrieve all categories from the "categories" table, ordered alphabetically by the category name
    @Query("SELECT * FROM categories ORDER BY categoryName ASC")
    suspend fun getAllCategories(): List<CategoryEntity>  // This retrieves all categories from the table, sorted by category name

}
