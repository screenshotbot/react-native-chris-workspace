package com.testapp

import android.graphics.Color
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

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
}
