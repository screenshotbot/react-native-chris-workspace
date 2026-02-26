package com.testapp

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.ImageView
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

        scenario.onActivity { activity ->
            val decorView = activity.window.decorView

            val insets = decorView.rootWindowInsets
            val topInset = insets?.systemWindowInsetTop ?: 0
            val bottomInset = insets?.systemWindowInsetBottom ?: 0

            val fullBitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
            decorView.draw(Canvas(fullBitmap))

            val cropHeight = fullBitmap.height - topInset - bottomInset
            val cropped = Bitmap.createBitmap(fullBitmap, 0, topInset, fullBitmap.width, cropHeight)
            fullBitmap.recycle()

            val density = activity.resources.displayMetrics.density
            val imageView = ImageView(activity)
            imageView.setImageBitmap(cropped)
            imageView.scaleType = ImageView.ScaleType.FIT_XY

            ViewHelpers.setupView(imageView)
                .setExactWidthDp((cropped.width / density).toInt())
                .setExactHeightDp((cropped.height / density).toInt())
                .layout()

            Screenshot.snap(imageView)
                .setName("actual_app_home")
                .record()

            cropped.recycle()
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

        Screenshot.snap(coinFlipView)
            .setName("coin_flip_heads")
            .record()
    }

    // Mimicking MyFeature.stories.tsx
    @Test
    fun testMyFeature() {
        val myFeatureView = createMyFeatureView(
            clickCount = 3
        )

        // Measure the view naturally with a constrained width
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(dpToPx(400), android.view.View.MeasureSpec.EXACTLY)
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        myFeatureView.measure(widthSpec, heightSpec)
        myFeatureView.layout(0, 0, myFeatureView.measuredWidth, myFeatureView.measuredHeight)

        Screenshot.snap(myFeatureView)
            .setName("my_feature")
            .record()
    }

    // Mimicking SwitchFeature.stories.tsx - Enabled state
    @Test
    fun testSwitchFeatureEnabled() {
        val switchFeatureView = createSwitchFeatureView(
            isEnabled = true
        )

        // Measure the view naturally with a constrained width
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(dpToPx(400), android.view.View.MeasureSpec.EXACTLY)
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        switchFeatureView.measure(widthSpec, heightSpec)
        switchFeatureView.layout(0, 0, switchFeatureView.measuredWidth, switchFeatureView.measuredHeight)

        Screenshot.snap(switchFeatureView)
            .setName("switch_feature_enabled")
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

    // Helper function to create MyFeature view
    private fun createMyFeatureView(clickCount: Int): LinearLayout {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(Color.WHITE)
        container.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))

        // Title
        val title = TextView(context)
        title.text = "My Feature"
        title.textSize = 20f
        title.setTextColor(Color.BLACK)
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleParams.bottomMargin = dpToPx(10)
        titleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        title.layoutParams = titleParams
        container.addView(title)

        // Button
        val button = Button(context)
        button.text = "Click Me!"
        button.setTextColor(Color.WHITE)
        button.isAllCaps = false

        val buttonDrawable = GradientDrawable()
        buttonDrawable.cornerRadius = dpToPx(4).toFloat()
        buttonDrawable.setColor(Color.parseColor("#007AFF"))
        button.background = buttonDrawable

        button.setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10))

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        button.layoutParams = buttonParams
        container.addView(button)

        // Counter text
        val counter = TextView(context)
        counter.text = "Clicked $clickCount times"
        counter.textSize = 16f
        counter.setTextColor(Color.BLACK)
        val counterParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        counterParams.topMargin = dpToPx(10)
        counterParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        counter.layoutParams = counterParams
        container.addView(counter)

        return container
    }

    // Helper function to create SwitchFeature view
    private fun createSwitchFeatureView(isEnabled: Boolean): LinearLayout {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(Color.WHITE)
        container.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))

        // Label
        val label = TextView(context)
        label.text = "Feature Switch"
        label.textSize = 18f
        label.setTextColor(Color.BLACK)
        val labelParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        labelParams.bottomMargin = dpToPx(10)
        labelParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        label.layoutParams = labelParams
        container.addView(label)

        // Switch visual representation (using a simple view to avoid animation issues)
        val switchView = TextView(context)
        switchView.text = if (isEnabled) "ON" else "OFF"
        switchView.textSize = 14f
        switchView.setTextColor(Color.WHITE)
        switchView.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8))

        val switchDrawable = GradientDrawable()
        switchDrawable.cornerRadius = dpToPx(20).toFloat()
        switchDrawable.setColor(if (isEnabled) Color.parseColor("#34C759") else Color.parseColor("#767577"))
        switchView.background = switchDrawable

        val switchParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        switchParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        switchView.layoutParams = switchParams
        container.addView(switchView)

        // Status text
        val status = TextView(context)
        status.text = if (isEnabled) "" else "False"
        status.textSize = 16f
        status.setTextColor(Color.BLACK)
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        statusParams.topMargin = dpToPx(10)
        statusParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        status.layoutParams = statusParams
        container.addView(status)

        // Conditional image (when enabled)
        if (isEnabled) {
            val imageView = android.widget.ImageView(context)
            // Create a placeholder colored rectangle since we can't load URLs in tests
            val placeholder = GradientDrawable()
            placeholder.setColor(Color.parseColor("#E0E0E0"))
            placeholder.cornerRadius = dpToPx(10).toFloat()
            imageView.background = placeholder

            // Add a text overlay to indicate this is a placeholder
            val imageContainer = android.widget.FrameLayout(context)
            val imageParams = android.widget.FrameLayout.LayoutParams(dpToPx(200), dpToPx(200))
            imageView.layoutParams = imageParams
            imageContainer.addView(imageView)

            val placeholderText = TextView(context)
            placeholderText.text = "Image\nPlaceholder"
            placeholderText.textSize = 16f
            placeholderText.setTextColor(Color.GRAY)
            placeholderText.gravity = android.view.Gravity.CENTER
            val textParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            placeholderText.layoutParams = textParams
            imageContainer.addView(placeholderText)

            val containerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            containerParams.topMargin = dpToPx(20)
            containerParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
            imageContainer.layoutParams = containerParams
            container.addView(imageContainer)
        }

        return container
    }

    // Helper function to convert dp to pixels
    private fun dpToPx(dp: Int): Int {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
