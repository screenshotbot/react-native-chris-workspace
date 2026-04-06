package com.rnstorybookautoscreenshots

import android.app.Application
import android.graphics.Color
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.Test
import org.junit.runner.RunWith
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
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

    @OptIn(UnstableReactNativeAPI::class)
    @Test
    fun constructViewTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
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
        val reactHost = ReactHostImpl(
            context,
            delegate,
            componentFactory,
            false, // allowPackagerServerAccess
            false, // useDevSupport
        )

        val surface = reactHost.createSurface(context, "SimpleTestComponent", null)
        assertEquals("SimpleTestComponent", surface.moduleName)
        assertNotNull(surface.view)

        val view = surface.view!!

        val renderLatch = CountDownLatch(1)
        val markerListener = ReactMarker.MarkerListener { name, _, _ ->
            if (name == ReactMarkerConstants.CONTENT_APPEARED) renderLatch.countDown()
        }
        ReactMarker.addListener(markerListener)

        var detacher: WindowAttachment.Detacher? = null

        try {
            instrumentation.runOnMainSync {
                reactHost.onHostResume(null)
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                view.setBackgroundColor(Color.WHITE)
                detacher = WindowAttachment.dispatchAttach(view)
                ViewHelpers.setupView(view).setExactWidthPx(1080).setExactHeightPx(1920).layout()
                surface.start()
            }

            assertTrue("Timed out waiting for first render", renderLatch.await(10, TimeUnit.SECONDS))
            Screenshot.snap(view).record()
        } finally {
            ReactMarker.removeListener(markerListener)
            instrumentation.runOnMainSync {
                surface.stop()
                detacher?.detach()
            }
        }
    }
}

fun assertGoodTask(ti : TaskInterface<Void>) {
    ti.waitForCompletion()
    assertFalse(ti.isFaulted())
    assertTrue(ti.isCompleted())
}
