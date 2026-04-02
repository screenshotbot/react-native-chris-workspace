package com.rnstorybookautoscreenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.testapp.MainApplication
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.Screenshot

@RunWith(AndroidJUnit4::class)
class IsolatedTest {
    @Test
    fun simpleTest() {
        assertTrue(true)
    }

    @Test
    fun constructViewTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as MainApplication
        val surface = app.reactHost.createSurface(context, "SimpleTestComponent", null)
        surface.start()
        assertNotNull(surface.view)

        ViewHelpers.setupView(surface.view!!)
            .setExactHeightPx(1000)
            .setExactWidthPx(1000)
            .layout()

        Screenshot.snap(surface.view!!)
            .record()
    }
}
