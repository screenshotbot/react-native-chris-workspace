package com.rnstorybookautoscreenshots

import android.graphics.Color
import android.widget.TextView
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IsolatedTest {

    @UiThreadTest
    @Test
    fun screenshotNativeView() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = TextView(context).apply {
            text = "Hello Screenshot"
            setBackgroundColor(Color.WHITE)
        }
        ViewHelpers.setupView(view).setExactWidthPx(400).setExactHeightPx(100).layout()
        Screenshot.snap(view).setName("native_view").record()
    }
}
