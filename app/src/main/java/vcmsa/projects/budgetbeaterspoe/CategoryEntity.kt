package vcmsa.projects.budgetbeaterspoe

import androidx.room.Entity  // Annotation to define this class as an entity for Room Database
import androidx.room.PrimaryKey  // Annotation to specify the primary key for the entity

// The CategoryEntity class represents the structure of the "categories" table in the database
@Entity(tableName = "categories")  // Specify the table name in the database
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)  // Automatically generate a unique ID for each category entry
    val id: Int = 0,  // The unique identifier for each category, automatically generated
    val categoryName: String,  // The name of the category
    val description: String? = null,  // Optional description of the category, can be null
    val maxLimit: Int,  // The maximum spending limit for the category
    val minLimit: Int  // The minimum spending limit for the category
)
