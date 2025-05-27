package vcmsa.projects.budgetbeaterspoe

import androidx.room.*

@Dao
interface ExpenseDao {

    // Method to insert a new expense, aborting if there's a conflict (e.g., duplicate ID)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity)

    // Method to delete multiple expenses by their IDs
    @Query("DELETE FROM expenses WHERE id IN (:expenseIds)")
    suspend fun deleteExpensesByIds(expenseIds: List<Int>)

    // Method to retrieve all expenses from the database, ordered by name in ascending order
    @Query("SELECT * FROM expenses ORDER BY name ASC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    // Method to retrieve a specific expense by its ID, returns null if not found
    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: Int): ExpenseEntity?

    // Method to update an existing expense in the database
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    // Method to delete all expenses from the database
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    // Method to delete an expense by its name (optional feature)
    @Query("DELETE FROM expenses WHERE name = :expenseName")
    suspend fun deleteExpenseByName(expenseName: String)

    // Method to delete a specific expense by its ID
    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Int)

    // Method to retrieve all expenses that fall within a specified date range
    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end")
    suspend fun getExpensesByDateRange(start: String, end: String): List<ExpenseEntity>

    // Method to retrieve all distinct categories from the expenses table
    @Query("SELECT DISTINCT category FROM expenses")
    suspend fun getAllCategories(): List<String>

    // Method to calculate the total amount spent in a specific category within a given date range
    @Query("""
        SELECT SUM(amount) FROM expenses 
        WHERE category = :category 
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalSpentForCategoryInRange(
        category: String,
        startDate: String,
        endDate: String
    ): Double?

    // Method to calculate the total amount spent in a specific category without a date range
    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category")
    suspend fun getTotalSpentForCategory(category: String): Float?
}
