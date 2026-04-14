package com.rnstorybookautoscreenshots

import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.testapp.MainApplication
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.WindowAttachment
import org.junit.Assert.*
import com.facebook.react.interfaces.*
import java.util.concurrent.CompletableFuture
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

        assertGoodTask(surface.prerender())

        assertNotNull(surface.view)

        val view = surface.view!!
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val ti = surface.start()
        assertGoodTask(ti)

        Screenshot.snap(surface.view!!)
            .record()
    }

    @Test
    fun childCountScreenshotTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as MainApplication
        val surface = app.reactHost.createSurface(context, "SimpleTestComponent", null)

        assertGoodTask(surface.prerender())

        val view = surface.view!! as ViewGroup
        var detacher: WindowAttachment.Detacher? = null
        var startTask: TaskInterface<Void>? = null

        try {
            instrumentation.runOnMainSync {
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                detacher = WindowAttachment.dispatchAttach(view)
                app.reactHost.onHostResume(null)
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                startTask = surface.start()
            }

            assertGoodTask(startTask!!)
            waitUntil { view.childCount > 0 }

            Screenshot.snap(view).record()
        } finally {
            instrumentation.runOnMainSync { detacher?.detach() }
        }
    }

    @Test
    fun childCountSyncTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as MainApplication
        val surface = app.reactHost.createSurface(context, "SimpleTestComponent", null)

        assertGoodTask(surface.prerender())

        val view = surface.view!! as ViewGroup
        var detacher: WindowAttachment.Detacher? = null

        var startTask: TaskInterface<Void>? = null

        try {
            instrumentation.runOnMainSync {
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                detacher = WindowAttachment.dispatchAttach(view)
                app.reactHost.onHostResume(null)
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                startTask = surface.start()
            }

            assertGoodTask(startTask!!)
            waitUntil { view.childCount > 0 }
            assertTrue("Expected childCount > 0, but was ${view.childCount}", view.childCount > 0)
        } finally {
            instrumentation.runOnMainSync { detacher?.detach() }
        }
    }

    @Test
    fun childCountTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as MainApplication
        val surface = app.reactHost.createSurface(context, "SimpleTestComponent", null)

        val view = surface.view!! as ViewGroup
        var detacher: WindowAttachment.Detacher? = null
        var startTask: TaskInterface<Void>? = null
        val childrenMounted = CompletableFuture<Int>()

        try {
            instrumentation.runOnMainSync {
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                detacher = WindowAttachment.dispatchAttach(view)
                app.reactHost.onHostResume(null)
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                startTask = surface.start()
                // dispatchAttach gives the view a real Handler on the main Looper via AttachInfo,
                // so view.post() routes to the main MessageQueue. Poll here (on the main thread,
                // between Choreographer frames) until Fabric mounts the first child.
                val check = object : Runnable {
                    override fun run() {
                        if (view.childCount > 0) childrenMounted.complete(view.childCount)
                        else view.postDelayed(this, 50)
                    }
                }
                view.post(check)
            }

            // start() fires on the bg executor. assertGoodTask blocks the test thread until
            // it completes, keeping the main thread free for Choreographer and our check loop.
            assertGoodTask(startTask!!)
            val count = childrenMounted.get(5, TimeUnit.SECONDS)
            assertTrue("Expected childCount > 0, but was $count", count > 0)
        } finally {
            instrumentation.runOnMainSync {
                surface.stop()
                detacher?.detach()
            }
        }
    }
}

fun waitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        check(System.currentTimeMillis() < deadline) { "Condition not met within ${timeoutMs}ms" }
        Thread.sleep(16)
    }
}

fun assertGoodTask(ti: TaskInterface<Void>) {
    ti.waitForCompletion()
    assertFalse(ti.isFaulted())
    assertTrue(ti.isCompleted())
}
