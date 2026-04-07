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
import com.facebook.react.interfaces.fabric.ReactSurface
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
     * Starts all [componentNames] surfaces simultaneously, waits for each JS component
     * to call ScreenshotHelper.takeScreenshot, then tears everything down.
     *
     * All surfaces are attached and started in a single runOnMainSync, so the JS thread
     * queues work for all of them at once instead of waiting for each test to finish
     * before the next begins.
     */
    @OptIn(UnstableReactNativeAPI::class)
    fun screenshotAll(componentNames: List<String>) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val latch = CountDownLatch(componentNames.size)
        ScreenshotHelperModule.sharedLatch = latch

        val surfaces = mutableListOf<ReactSurface>()
        val detachers = mutableListOf<WindowAttachment.Detacher?>()

        componentNames.forEach { name ->
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
            val surface = reactHost.createSurface(context, name, null)
            assertNotNull("No view for $name", surface.view)
            ScreenshotHelperModule.pendingViews[name] = WeakReference(surface.view!!)
            surfaces.add(surface)
            detachers.add(null)
        }

        try {
            instrumentation.runOnMainSync {
                surfaces.forEachIndexed { i, surface ->
                    val view = surface.view!!
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    view.setBackgroundColor(Color.WHITE)
                    detachers[i] = WindowAttachment.dispatchAttach(view)
                    ViewHelpers.setupView(view).setExactWidthPx(1080).setExactHeightPx(1920).layout()
                    surface.start()
                }
            }

            assertTrue(
                "Timed out waiting for all screenshots: $componentNames",
                latch.await(60, TimeUnit.SECONDS)
            )
        } finally {
            ScreenshotHelperModule.pendingViews.clear()
            ScreenshotHelperModule.sharedLatch = null
            instrumentation.runOnMainSync {
                surfaces.forEach { it.stop() }
                detachers.forEach { it?.detach() }
            }
        }
    }

    fun screenshotComponent(componentName: String) = screenshotAll(listOf(componentName))
}
