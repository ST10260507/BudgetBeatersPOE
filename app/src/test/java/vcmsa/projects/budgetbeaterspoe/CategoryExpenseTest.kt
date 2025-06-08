package vcmsa.projects.budgetbeaterspoe

import org.junit.Assert.*
import org.junit.Test

class CategoryExpenseTest {

    @Test
    fun testAddingExpensesWithinCategory() {
        val foodExpenses = listOf(50.0, 25.5, 10.0)
        val total = foodExpenses.sum()
        val limit = 100.0

        assertTrue("Total should be within limit", total <= limit)
        assertEquals(85.5, total, 0.001)
    }
}
