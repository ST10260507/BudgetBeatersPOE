package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Variables to track touch events for swipe detection
    private var x1 = 0f
    private var x2 = 0f
    private val MIN_DISTANCE = 150 // Minimum distance for detecting a swipe

    @SuppressLint("ClickableViewAccessibility") // Suppress lint warning for using touch listener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set the layout for the activity

        // Get the main layout view
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)

        // Set up a touch listener on the main layout to detect swipe actions
        mainLayout.setOnTouchListener { _, event ->
            when (event.action) {
                // ACTION_DOWN: User touches the screen
                MotionEvent.ACTION_DOWN -> {
                    x1 = event.x // Store the initial x-coordinate of the touch
                    true
                }
                // ACTION_UP: User lifts their finger from the screen
                MotionEvent.ACTION_UP -> {
                    x2 = event.x // Store the x-coordinate when the touch is released
                    val deltaX = x2 - x1 // Calculate the horizontal distance between the touch start and end
                    if (kotlin.math.abs(deltaX) > MIN_DISTANCE) { // Check if the swipe distance is greater than the minimum threshold
                        if (x2 > x1) { // If the swipe is from left to right
                            // Right swipe detected, navigate to the LoginActivity
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent) // Start the LoginActivity
                            // Apply sliding transition animation between activities
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }
}
