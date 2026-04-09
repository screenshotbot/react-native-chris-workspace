package com.rnstorybookautoscreenshots

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.testapp.MainApplication
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.WindowAttachment
import org.junit.Assert.*;
import com.facebook.react.interfaces.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    @Test
    fun addViewHookTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as MainApplication
        val surface = app.reactHost.createSurface(context, "SimpleTestComponent", null)

        val view = surface.view!! as android.view.ViewGroup
        val latch = CountDownLatch(1)
        var detacher: WindowAttachment.Detacher? = null

        try {
            instrumentation.runOnMainSync {
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                view.setBackgroundColor(Color.WHITE)
                view.setOnHierarchyChangeListener(object : android.view.ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View, child: View) {
                        view.setOnHierarchyChangeListener(null)
                        latch.countDown()
                    }
                    override fun onChildViewRemoved(parent: View, child: View) {}
                })
                detacher = WindowAttachment.dispatchAttach(view)
                app.reactHost.onHostResume(null)
                ViewHelpers.setupView(view).setExactWidthPx(1080).setExactHeightPx(1920).layout()
                surface.start()
            }

            assertTrue(
                "Timed out waiting for Fabric to mount first child",
                latch.await(30, TimeUnit.SECONDS)
            )

            instrumentation.runOnMainSync {
                ViewHelpers.setupView(view).setExactWidthPx(1080).setExactHeightPx(1920).layout()
                Screenshot.snap(view).setName("addViewHookTest").record()
            }
        } finally {
            instrumentation.runOnMainSync {
                surface.stop()
                detacher?.detach()
            }
        }
    }
}

class FirstChildLatch(context: Context) : FrameLayout(context) {
    val latch = CountDownLatch(1)

    override fun addView(child: View, index: Int, params: android.view.ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        latch.countDown()
    }
}

fun assertGoodTask(ti : TaskInterface<Void>) {
    ti.waitForCompletion()
    assertFalse(ti.isFaulted())
    assertTrue(ti.isCompleted())
}
