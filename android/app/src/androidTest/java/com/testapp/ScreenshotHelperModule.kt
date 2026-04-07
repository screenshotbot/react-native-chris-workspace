package com.testapp

import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.testing.screenshot.Screenshot
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

/**
 * Native module called from JS when a component has finished rendering.
 *
 * [pendingViews] maps component name → view to screenshot.
 * [sharedLatch] is counted down once per component that calls back,
 * so the test thread can await all of them concurrently.
 */
class ScreenshotHelperModule(reactContext: ReactApplicationContext)
    : ReactContextBaseJavaModule(reactContext) {

    companion object {
        val pendingViews = ConcurrentHashMap<String, WeakReference<View>>()
        var sharedLatch: CountDownLatch? = null
    }

    override fun getName() = "ScreenshotHelper"

    @ReactMethod
    fun takeScreenshot(name: String) {
        val view = pendingViews[name]?.get()
        if (view == null) {
            sharedLatch?.countDown()
            return
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            Screenshot.snap(view).setName(name).record()
        }
        sharedLatch?.countDown()
    }
}
