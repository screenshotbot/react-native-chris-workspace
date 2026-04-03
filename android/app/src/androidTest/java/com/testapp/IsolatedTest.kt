package com.rnstorybookautoscreenshots

import android.Manifest
import android.app.Application
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.facebook.react.PackageList
import com.facebook.react.bridge.ReactMarker
import com.facebook.react.bridge.ReactMarkerConstants
import com.facebook.react.common.annotations.UnstableReactNativeAPI
import com.facebook.react.bridge.JSBundleLoader
import com.facebook.react.defaults.DefaultComponentsRegistry
import com.facebook.react.defaults.DefaultReactHostDelegate
import com.facebook.react.defaults.DefaultTurboModuleManagerDelegate
import com.facebook.react.fabric.ComponentFactory
import com.facebook.react.runtime.ReactHostImpl
import com.facebook.react.runtime.hermes.HermesInstance
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.facebook.testing.screenshot.Screenshot
import org.junit.Assert.*;
import com.facebook.react.interfaces.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class IsolatedTest {

    @get:Rule
    val overlayPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.SYSTEM_ALERT_WINDOW)

    @Test
    fun simpleTest() {
        assertTrue(true)
    }

    @OptIn(UnstableReactNativeAPI::class)
    @Test
    fun constructViewTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val reactHost = createTestReactHost(context)

        val renderLatch = CountDownLatch(1)
        val markerListener = ReactMarker.MarkerListener { name, _, _ ->
            if (name == ReactMarkerConstants.CONTENT_APPEARED) renderLatch.countDown()
        }
        ReactMarker.addListener(markerListener)

        val surface = reactHost.createSurface(context, "SimpleTestComponent", null)
        assertEquals("SimpleTestComponent", surface.moduleName)
        assertNotNull(surface.view)

        val view = surface.view!!
        val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            1080,
            1920,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // alpha=0 so the compositor skips drawing while Fabric still sees a real Window.
            alpha = 0f
        }

        try {
            instrumentation.runOnMainSync {
                // RESUMED state is required for Fabric to commit mutations to the view.
                reactHost.onHostResume(null)
                // Software rendering so Screenshot.snap() can capture via draw(canvas).
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                view.setBackgroundColor(Color.WHITE)
                wm.addView(view, params)
                surface.start()
            }

            assertTrue("Timed out waiting for first render", renderLatch.await(10, TimeUnit.SECONDS))

            Screenshot.snap(view).record()
        } finally {
            ReactMarker.removeListener(markerListener)
            instrumentation.runOnMainSync {
                surface.stop()
                wm.removeView(view)
            }
        }
    }
}

@OptIn(UnstableReactNativeAPI::class)
private fun createTestReactHost(context: android.content.Context): ReactHostImpl {
    val app = context.applicationContext as Application
    val bundleLoader = JSBundleLoader.createAssetLoader(
        context, "assets://index.android.bundle", true)
    val delegate = DefaultReactHostDelegate(
        jsMainModulePath = "index",
        jsBundleLoader = bundleLoader,
        reactPackages = PackageList(app).packages,
        jsRuntimeFactory = HermesInstance(),
        turboModuleManagerDelegateBuilder = DefaultTurboModuleManagerDelegate.Builder(),
        exceptionHandler = { throw it },
    )
    val componentFactory = ComponentFactory()
    DefaultComponentsRegistry.register(componentFactory)
    return ReactHostImpl(
        context,
        delegate,
        componentFactory,
        false, // allowPackagerServerAccess
        false, // useDevSupport
    )
}

fun assertGoodTask(ti : TaskInterface<Void>) {
    ti.waitForCompletion()
    assertFalse(ti.isFaulted())
    assertTrue(ti.isCompleted())
}
