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
import org.junit.Assert.*;
import com.facebook.react.interfaces.*

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
        assertEquals("SimpleTestComponent", surface.moduleName)

        // TODO: we aren't 100% sure if prerender() and start() are being called the way we want it to.
        // We probably want to create a ReactHost directly instead of taking it from the MainApplication... probably
        // Also look up bridge-less mode.
        assertGoodTask(surface.prerender())


        assertNotNull(surface.view)

        ViewHelpers.setupView(surface.view!!)
            .setExactHeightPx(1000)
            .setExactWidthPx(1000)
            .layout()


        val ti = surface.start()
        assertGoodTask(ti)

        Screenshot.snap(surface.view!!)
            .record()
    }
}

fun assertGoodTask(ti : TaskInterface<Void>) {
    ti.waitForCompletion()
    assertFalse(ti.isFaulted())
    assertTrue(ti.isCompleted())
}
