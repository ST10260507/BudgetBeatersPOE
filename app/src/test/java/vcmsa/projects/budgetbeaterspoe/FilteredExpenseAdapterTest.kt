package vcmsa.projects.budgetbeaterspoe

import org.junit.Assert.assertEquals
import org.junit.Test

class FilteredExpenseAdapterTest {

    @Test
    fun `adapter returns correct item count`() {
        val sampleData = listOf(
            ExpenseEntity(
                id = "1", // ID should be a String
                name = "Vegetables",
                category = "Groceries",
                categoryId = "cat_groc", // Assuming a category ID
                amount = 120.0,
                date = "2024-05-01",
                userId = "user123", // Assuming a user ID
                imageUrl = null,
                description = "Daily groceries"
            ),
            ExpenseEntity(
                id = "2",
                name = "Taxi",
                category = "Transportation",
                categoryId = "cat_trans",
                amount = 80.0,
                date = "2024-05-02",
                userId = "user123",
                imageUrl = null,
                description = "Work commute"
            ),
            ExpenseEntity(
                id = "3",
                name = "Movie",
                category = "Entertainment",
                categoryId = "cat_ent",
                amount = 50.0,
                date = "2024-05-03",
                userId = "user123",
                imageUrl = null,
                description = "Evening movie"
            )
        )

        // Assuming FilteredExpenseAdapter takes a List<ExpenseEntity> in its constructor
        val adapter = FilteredExpenseAdapter(sampleData)

        assertEquals(3, adapter.itemCount)
    }
}