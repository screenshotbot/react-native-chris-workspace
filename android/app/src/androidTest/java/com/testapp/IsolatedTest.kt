package com.rnstorybookautoscreenshots

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.testing.screenshot.Screenshot
import com.testapp.MainApplication
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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
    }

    @Test
    fun screenshotPlainView() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = View(context).also {
            it.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        Screenshot.snap(view).setName("plain_view").record()
    }
}
