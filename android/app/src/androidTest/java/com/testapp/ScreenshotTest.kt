package com.testapp

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
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

    // Test that launches the actual app and takes a screenshot
    @Test
    fun testActualApp() {
        // Launch the main activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Wait for the app to fully load (React Native takes time to start)
        Thread.sleep(15000)

        // Take screenshot of the activity
        scenario.onActivity { activity ->
            val rootView = activity.window.decorView.rootView
            Screenshot.snap(rootView)
                .setName("actual_app_home")
                .record()
        }

        scenario.close()
    }

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

        // Measure the view naturally with a constrained width
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(dpToPx(400), android.view.View.MeasureSpec.EXACTLY)
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        timerView.measure(widthSpec, heightSpec)
        timerView.layout(0, 0, timerView.measuredWidth, timerView.measuredHeight)

        // Log the dimensions for verification
        android.util.Log.d("ScreenshotTest", "Timer view dimensions: ${timerView.measuredWidth} x ${timerView.measuredHeight}")

        // Also save to external storage for easier access
        val bitmap = android.graphics.Bitmap.createBitmap(
            timerView.measuredWidth,
            timerView.measuredHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        timerView.draw(canvas)

        val file = java.io.File("/sdcard/Download/timer_paused_full.png")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        android.util.Log.d("ScreenshotTest", "Screenshot saved to: ${file.absolutePath}")

        Screenshot.snap(timerView)
            .setName("timer_paused")
            .record()
    }

    // Mimicking CoinFlipFeature.stories.tsx - Heads result
    @Test
    fun testCoinFlipFeatureHeads() {
        val coinFlipView = createCoinFlipView(
            result = "HEADS",
            flips = 5
        )

        // Measure the view naturally with a constrained width
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(dpToPx(400), android.view.View.MeasureSpec.EXACTLY)
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        coinFlipView.measure(widthSpec, heightSpec)
        coinFlipView.layout(0, 0, coinFlipView.measuredWidth, coinFlipView.measuredHeight)

        // Log the dimensions for verification
        android.util.Log.d("ScreenshotTest", "CoinFlip view dimensions: ${coinFlipView.measuredWidth} x ${coinFlipView.measuredHeight}")

        // Save to external storage
        val bitmap = android.graphics.Bitmap.createBitmap(
            coinFlipView.measuredWidth,
            coinFlipView.measuredHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        coinFlipView.draw(canvas)

        val file = java.io.File("/sdcard/Download/coin_flip_heads.png")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        android.util.Log.d("ScreenshotTest", "Screenshot saved to: ${file.absolutePath}")

        Screenshot.snap(coinFlipView)
            .setName("coin_flip_heads")
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

    // Helper function to create a coin flip view mimicking CoinFlipFeature component
    private fun createCoinFlipView(result: String, flips: Int): LinearLayout {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(Color.WHITE)
        container.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))

        // Title
        val title = TextView(context)
        title.text = "Coin Flip"
        title.textSize = 20f
        title.setTextColor(Color.BLACK)
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleParams.bottomMargin = dpToPx(20)
        titleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        title.layoutParams = titleParams
        container.addView(title)

        // Coin (circular view)
        val coin = TextView(context)
        coin.text = result
        coin.textSize = 32f
        coin.setTextColor(Color.WHITE)
        coin.gravity = android.view.Gravity.CENTER
        coin.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))

        val coinDrawable = GradientDrawable()
        coinDrawable.shape = GradientDrawable.OVAL
        coinDrawable.setColor(if (result == "HEADS") Color.parseColor("#FFD700") else Color.parseColor("#C0C0C0"))
        coin.background = coinDrawable

        val coinParams = LinearLayout.LayoutParams(dpToPx(120), dpToPx(120))
        coinParams.topMargin = dpToPx(20)
        coinParams.bottomMargin = dpToPx(20)
        coinParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        coin.layoutParams = coinParams
        container.addView(coin)

        // Flip button
        val flipButton = Button(context)
        flipButton.text = "Flip Coin"
        flipButton.setTextColor(Color.WHITE)
        flipButton.textSize = 18f
        flipButton.isAllCaps = false

        val buttonDrawable = GradientDrawable()
        buttonDrawable.cornerRadius = dpToPx(8).toFloat()
        buttonDrawable.setColor(Color.parseColor("#007AFF"))
        flipButton.background = buttonDrawable

        flipButton.setPadding(dpToPx(40), dpToPx(15), dpToPx(40), dpToPx(15))

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonParams.topMargin = dpToPx(10)
        buttonParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        flipButton.layoutParams = buttonParams
        container.addView(flipButton)

        // Stats container
        val statsContainer = LinearLayout(context)
        statsContainer.orientation = LinearLayout.VERTICAL
        val statsParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        statsParams.topMargin = dpToPx(20)
        statsParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        statsContainer.layoutParams = statsParams

        // Total flips text
        val flipsText = TextView(context)
        flipsText.text = "Total Flips: $flips"
        flipsText.textSize = 16f
        flipsText.setTextColor(Color.BLACK)
        val flipsParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        flipsParams.bottomMargin = dpToPx(5)
        flipsText.layoutParams = flipsParams
        statsContainer.addView(flipsText)

        // Result text
        val resultText = TextView(context)
        resultText.text = "Result: $result"
        resultText.textSize = 18f
        resultText.setTextColor(Color.BLACK)
        statsContainer.addView(resultText)

        container.addView(statsContainer)

        return container
    }

    // Helper function to convert dp to pixels
    private fun dpToPx(dp: Int): Int {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
