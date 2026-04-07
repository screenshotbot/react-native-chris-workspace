package com.rnstorybookautoscreenshots

import android.app.Application
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.PackageList
import com.facebook.react.bridge.JSBundleLoader
import com.facebook.react.bridge.ReactMarker
import com.facebook.react.bridge.ReactMarkerConstants
import com.facebook.react.common.annotations.UnstableReactNativeAPI
import com.facebook.react.defaults.DefaultComponentsRegistry
import com.facebook.react.defaults.DefaultReactHostDelegate
import com.facebook.react.defaults.DefaultTurboModuleManagerDelegate
import com.facebook.react.fabric.ComponentFactory
import com.facebook.react.runtime.ReactHostImpl
import com.facebook.react.runtime.hermes.HermesInstance
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.WindowAttachment
import com.testapp.R
import junit.framework.TestCase.assertNotNull
import org.junit.Assert.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Shared ReactHostImpl for instrumentation tests.
 *
 * DefaultComponentsRegistry.register is a native JNI call that must only be invoked once
 * per process. This singleton ensures all test classes share one ReactHostImpl instance
 * instead of each creating their own.
 */
@OptIn(UnstableReactNativeAPI::class)
object ReactHostFixture {
    val reactHost: ReactHostImpl by lazy {
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
        val host = ReactHostImpl(context, delegate, componentFactory, false, false)
        instrumentation.runOnMainSync { host.onHostResume(null) }
        host
    }

    /**
     * Creates a surface for [componentName], waits for React to render via
     * ReactMarker.CONTENT_APPEARED, snaps a screenshot, then tears down.
     *
     * Uses a themed context so Android widgets (Switch, etc.) can resolve
     * their styled drawables.
     */
    @OptIn(UnstableReactNativeAPI::class)
    fun screenshotComponent(componentName: String, screenshotName: String = componentName) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)

        val surface = reactHost.createSurface(context, componentName, null)
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
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                view.setBackgroundColor(Color.WHITE)
                detacher = WindowAttachment.dispatchAttach(view)
                ViewHelpers.setupView(view).setExactWidthPx(1080).setExactHeightPx(1920).layout()
                surface.start()
            }

            assertTrue(
                "Timed out waiting for render: $componentName",
                renderLatch.await(10, TimeUnit.SECONDS)
            )
            Screenshot.snap(view).setName(screenshotName).record()
        } finally {
            ReactMarker.removeListener(markerListener)
            instrumentation.runOnMainSync {
                surface.stop()
                detacher?.detach()
            }
        }
    }
}
