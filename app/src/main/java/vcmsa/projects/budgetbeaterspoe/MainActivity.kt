package vcmsa.projects.budgetbeaterspoe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log // Import for logging
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import vcmsa.projects.budgetbeaterspoe.databinding.ActivityMainBinding // Not used directly, but good to keep if it was meant to be used

/**
 * The main entry activity of the application.
 * This activity primarily serves as a splash screen or an initial screen
 * that detects a right swipe gesture to navigate to the LoginActivity.
 */
class MainActivity : AppCompatActivity() {

    // TAG for logging messages related to this activity
    private val TAG = "MainActivity"

    // Variables to track touch events for swipe detection
    private var x1 = 0f // Initial x-coordinate of the touch event
    private var x2 = 0f // Final x-coordinate of the touch event

    // Minimum distance in pixels for detecting a horizontal swipe
    private val MIN_DISTANCE = 150

    // Suppress lint warning for using setOnTouchListener directly on a view
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started.") // Log activity creation

        // Set the layout for the activity
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate: Layout 'activity_main' set.")

        // Enable edge-to-edge display for a more immersive experience
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: Edge-to-edge enabled.")

        // Get the main ConstraintLayout view from the layout
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        Log.d(TAG, "onCreate: Main layout found.")

        // Set up a touch listener on the main layout to detect swipe actions
        mainLayout.setOnTouchListener { v, event ->
            when (event.action) {
                // ACTION_DOWN: User touches the screen
                MotionEvent.ACTION_DOWN -> {
                    x1 = event.x // Store the initial x-coordinate of the touch
                    Log.d(TAG, "onTouch: ACTION_DOWN at x1=$x1") // Log touch down event
                    true // Consume the event
                }
                // ACTION_UP: User lifts their finger from the screen
                MotionEvent.ACTION_UP -> {
                    x2 = event.x // Store the x-coordinate when the touch is released
                    val deltaX = x2 - x1 // Calculate the horizontal distance between touch start and end
                    Log.d(TAG, "onTouch: ACTION_UP at x2=$x2, deltaX=$deltaX") // Log touch up event and delta

                    // Check if the absolute swipe distance is greater than the minimum threshold
                    if (kotlin.math.abs(deltaX) > MIN_DISTANCE) {
                        Log.d(TAG, "onTouch: Swipe distance ($deltaX) exceeds MIN_DISTANCE ($MIN_DISTANCE).")
                        // If the swipe is from left to right (x2 > x1)
                        if (x2 > x1) {
                            Log.d(TAG, "onTouch: Right swipe detected.") // Log right swipe detection
                            // Right swipe detected, navigate to the LoginActivity
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent) // Start the LoginActivity
                            // Apply sliding transition animation: new activity slides in from left, current slides out to right
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            Log.d(TAG, "onTouch: Navigated to LoginActivity with slide transition.")
                        } else {
                            Log.d(TAG, "onTouch: Left swipe detected (not handled for navigation).") // Log left swipe
                        }
                    } else {
                        Log.d(TAG, "onTouch: Swipe distance ($deltaX) is less than MIN_DISTANCE ($MIN_DISTANCE). Not a significant swipe.")
                    }
                    true // Consume the event
                }
                else -> {
                    Log.d(TAG, "onTouch: Unhandled MotionEvent action: ${event.action}") // Log unhandled actions
                    false // Do not consume the event
                }
            }
        }

        // Set up window insets listener for system bars (status bar, navigation bar)
        // This ensures the layout content adjusts around system UI elements.
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the view to avoid content overlapping with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }
        Log.d(TAG, "onCreate: Window insets listener set for main layout.")
    }
}