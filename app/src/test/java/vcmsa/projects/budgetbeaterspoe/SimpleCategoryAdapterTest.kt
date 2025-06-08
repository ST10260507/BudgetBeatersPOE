package vcmsa.projects.budgetbeaterspoe

import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleCategoryAdapterTest {

    @Test
    fun `adapter returns correct item count`() {
        val sampleCategories = listOf(
            CategoryEntity(
                id = "1", // ID should be a String
                categoryName = "Food",
                maxLimit = 700,
                minLimit = 250,
                userId = "user123" // userId should be a String
            ),
            CategoryEntity(
                id = "2",
                categoryName = "Transport",
                maxLimit = 300,
                minLimit = 150,
                userId = "user123"
            ),
            CategoryEntity(
                id = "3",
                categoryName = "Entertainment",
                maxLimit = 250,
                minLimit = 75,
                userId = "user123"
            )
        )

        // Assuming SimpleCategoryAdapter takes a List<CategoryEntity> in its constructor
        val adapter = SimpleCategoryAdapter(sampleCategories)

        assertEquals(3, adapter.itemCount)
    }
}