package com.rnstorybookautoscreenshots

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
import com.facebook.testing.screenshot.Screenshot
import com.testapp.MainApplication
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class IsolatedTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )

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
    fun screenshotSimpleTestComponent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as MainApplication

        val themedContext = ContextThemeWrapper(context, context.applicationInfo.theme)
        val surface = app.reactHost.createSurface(themedContext, "SimpleTestComponent", null)
        val view = surface.view
            ?: throw IllegalStateException("ReactSurface returned a null view")

        val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            1080,
            1920,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { alpha = 0f }

        instrumentation.runOnMainSync {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            wm.addView(view, params)
            surface.start()
        }

        // Wait for JS to load and Fabric to mount children (up to 30s for cold start)
        val deadline = System.currentTimeMillis() + 30_000L
        while (System.currentTimeMillis() < deadline) {
            waitTwoFrames()
            var childCount = 0
            instrumentation.runOnMainSync { childCount = view.childCount }
            if (childCount > 0) break
        }

        instrumentation.runOnMainSync {
            // Child views added by Fabric default to hardware acceleration;
            // set software layer recursively so Screenshot.snap() can capture them.
            setLayerTypeSoftwareRecursively(view)
            Screenshot.snap(view).setName("simple_test_component").record()
        }

        instrumentation.runOnMainSync {
            surface.stop()
            wm.removeView(view)
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

    private fun waitTwoFrames() {
        repeat(2) {
            val latch = CountDownLatch(1)
            Handler(Looper.getMainLooper()).post {
                Choreographer.getInstance().postFrameCallback { latch.countDown() }
            }
            latch.await()
        }
    }
}
