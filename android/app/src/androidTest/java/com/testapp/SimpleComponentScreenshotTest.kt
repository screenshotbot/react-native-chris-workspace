package com.testapp

import android.Manifest
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.facebook.react.ReactApplication
import com.facebook.testing.screenshot.Screenshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class SimpleComponentScreenshotTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )

    @Test
    fun screenshotSimpleComponent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ReactApplication
        val reactHost = app.reactHost!!

        val context = ContextThemeWrapper(
            instrumentation.targetContext,
            instrumentation.targetContext.applicationInfo.theme
        )
        val surface = reactHost.createSurface(context, "SimpleComponent", null)
        val view = surface.view
            ?: throw IllegalStateException("ReactSurface returned a null view")

        val wm = instrumentation.targetContext
            .getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            1080, 1920,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        instrumentation.runOnMainSync {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            wm.addView(view, params)
            surface.start()
        }

        waitTwoFrames()

        instrumentation.runOnMainSync {
            setLayerTypeSoftwareRecursively(view)
            Screenshot.snap(view).setName("simple_component").record()
        }

        instrumentation.runOnMainSync {
            surface.stop()
            wm.removeView(view)
        }
    }

    @Test
    fun fabricChildrenMountedAfterTwoFrames() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ReactApplication
        val reactHost = app.reactHost!!

        val context = ContextThemeWrapper(
            instrumentation.targetContext,
            instrumentation.targetContext.applicationInfo.theme
        )
        val surface = reactHost.createSurface(context, "SimpleComponent", null)
        val view = surface.view as ViewGroup
            ?: throw IllegalStateException("ReactSurface returned a null view")

        val wm = instrumentation.targetContext
            .getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            1080, 1920,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        instrumentation.runOnMainSync {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            wm.addView(view, params)
            surface.start()
        }

        var childCountBefore = -1
        instrumentation.runOnMainSync { childCountBefore = view.childCount }
        assertEquals("Fabric should not have mounted children yet", 0, childCountBefore)

        waitTwoFrames()

        var childCountAfter = -1
        instrumentation.runOnMainSync {
            childCountAfter = view.childCount
            surface.stop()
            wm.removeView(view)
        }
        assertTrue("Fabric should have mounted children after two frames", childCountAfter > 0)
    }

    private fun waitTwoFrames() {
        repeat(2) {
            val latch = CountDownLatch(1)
            Handler(Looper.getMainLooper()).post {
                Choreographer.getInstance().postFrameCallback { latch.countDown() }
            }
            latch.await()
        }
    }

    private fun setLayerTypeSoftwareRecursively(view: View) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setLayerTypeSoftwareRecursively(view.getChildAt(i))
            }
        }
    }
}
