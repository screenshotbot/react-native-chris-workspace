package com.testapp

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun testSimpleTextView() {
        val view = TextView(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        view.text = "Hello Screenshot Test"
        view.setTextColor(Color.BLACK)
        view.setBackgroundColor(Color.WHITE)
        view.setPadding(20, 20, 20, 20)

        // Measure and layout the view
        ViewHelpers.setupView(view)
            .setExactWidthDp(300)
            .setExactHeightDp(100)
            .layout()

        // Take screenshot
        Screenshot.snap(view)
            .setName("simple_text_view")
            .record()
    }

    // Mimicking TimerFeature.stories.tsx - Paused state
    @Test
    fun testTimerFeaturePaused() {
        val timerView = createTimerView(
            seconds = 125,
            isRunning = false
        )

        ViewHelpers.setupView(timerView)
            .setExactWidthDp(350)
            .setExactHeightDp(300)
            .layout()

        Screenshot.snap(timerView)
            .setName("timer_paused")
            .record()
    }

    // Helper function to create a timer view mimicking TimerFeature component
    private fun createTimerView(seconds: Int, isRunning: Boolean): LinearLayout {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(Color.WHITE)
        container.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))

        // Title
        val title = TextView(context)
        title.text = "Timer"
        title.textSize = 20f
        title.setTextColor(Color.BLACK)
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleParams.bottomMargin = dpToPx(15)
        title.layoutParams = titleParams
        container.addView(title)

        // Time display
        val timeDisplay = TextView(context)
        timeDisplay.text = formatTime(seconds)
        timeDisplay.textSize = 48f
        timeDisplay.setTextColor(Color.BLACK)
        timeDisplay.gravity = android.view.Gravity.CENTER
        val timeParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        timeParams.topMargin = dpToPx(20)
        timeParams.bottomMargin = dpToPx(20)
        timeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        timeDisplay.layoutParams = timeParams
        container.addView(timeDisplay)

        // Button container (horizontal layout)
        val buttonContainer = LinearLayout(context)
        buttonContainer.orientation = LinearLayout.HORIZONTAL
        val buttonContainerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonContainerParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        buttonContainer.layoutParams = buttonContainerParams

        // Start/Pause button
        val startPauseButton = createTimerButton(
            context,
            label = if (isRunning) "Pause" else "Start",
            color = if (isRunning) "#FF9500" else "#007AFF"
        )
        val startParams = LinearLayout.LayoutParams(
            dpToPx(100),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        startParams.rightMargin = dpToPx(15)
        startPauseButton.layoutParams = startParams
        buttonContainer.addView(startPauseButton)

        // Reset button
        val resetButton = createTimerButton(
            context,
            label = "Reset",
            color = "#FF3B30"
        )
        val resetParams = LinearLayout.LayoutParams(
            dpToPx(100),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        resetButton.layoutParams = resetParams
        buttonContainer.addView(resetButton)

        container.addView(buttonContainer)

        return container
    }

    // Helper function to create a styled button for the timer
    private fun createTimerButton(context: android.content.Context, label: String, color: String): Button {
        val button = Button(context)
        button.text = label
        button.setTextColor(Color.WHITE)
        button.textSize = 16f
        button.isAllCaps = false

        val drawable = GradientDrawable()
        drawable.cornerRadius = dpToPx(8).toFloat()
        drawable.setColor(Color.parseColor(color))
        button.background = drawable

        button.setPadding(dpToPx(30), dpToPx(12), dpToPx(30), dpToPx(12))

        return button
    }

    // Helper function to format seconds to HH:MM:SS
    private fun formatTime(totalSeconds: Int): String {
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hrs, mins, secs)
    }

    // Helper function to convert dp to pixels
    private fun dpToPx(dp: Int): Int {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
