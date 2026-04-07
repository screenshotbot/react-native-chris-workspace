package com.rnstorybookautoscreenshots

import android.app.Application
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.PackageList
import com.facebook.react.bridge.JSBundleLoader
import com.facebook.react.common.annotations.UnstableReactNativeAPI
import com.facebook.react.defaults.DefaultComponentsRegistry
import com.facebook.react.defaults.DefaultReactHostDelegate
import com.facebook.react.defaults.DefaultTurboModuleManagerDelegate
import com.facebook.react.fabric.ComponentFactory
import com.facebook.react.runtime.ReactHostImpl
import com.facebook.react.runtime.hermes.HermesInstance
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.WindowAttachment
import com.testapp.R
import com.testapp.ScreenshotHelperModule
import com.testapp.ScreenshotHelperPackage
import junit.framework.TestCase.assertNotNull
import org.junit.Assert.assertTrue
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
            reactPackages = PackageList(app).packages + listOf(ScreenshotHelperPackage()),
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
     * Creates a surface for [componentName], waits for the JS component to call
     * ScreenshotHelper.takeScreenshot (via useEffect), then tears down.
     *
     * The screenshot is taken inside the native module on the JS callback, so the
     * timing is driven by the JS render cycle rather than native heuristics.
     */
    @OptIn(UnstableReactNativeAPI::class)
    fun screenshotComponent(componentName: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)

        val surface = reactHost.createSurface(context, componentName, null)
        assertNotNull(surface.view)
        val view = surface.view!!

        val latch = CountDownLatch(1)
        ScreenshotHelperModule.pendingView = WeakReference(view)
        ScreenshotHelperModule.pendingLatch = latch

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
                "Timed out waiting for JS to call takeScreenshot: $componentName",
                latch.await(10, TimeUnit.SECONDS)
            )
        } finally {
            ScreenshotHelperModule.pendingView = null
            ScreenshotHelperModule.pendingLatch = null
            instrumentation.runOnMainSync {
                surface.stop()
                detacher?.detach()
            }
        }
    }
}
