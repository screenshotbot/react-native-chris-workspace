package com.testapp

import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.testing.screenshot.Screenshot
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

/**
 * Native module called from JS when a component has finished rendering.
 *
 * The test sets [pendingView] and [pendingLatch] before starting each surface.
 * When JS calls [takeScreenshot], the module snaps the view and releases the latch
 * so the test thread can unblock and clean up.
 */
class ScreenshotHelperModule(reactContext: ReactApplicationContext)
    : ReactContextBaseJavaModule(reactContext) {

    companion object {
        var pendingView: WeakReference<View>? = null
        var pendingLatch: CountDownLatch? = null
    }

    override fun getName() = "ScreenshotHelper"

    @ReactMethod
    fun takeScreenshot(name: String) {
        val view = pendingView?.get()
        if (view == null) {
            pendingLatch?.countDown()
            return
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            Screenshot.snap(view).setName(name).record()
        }
        pendingLatch?.countDown()
    }
}
