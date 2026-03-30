package com.rnstorybookautoscreenshots

import android.Manifest
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.facebook.react.ReactApplication
import com.facebook.react.ReactRootView
import com.facebook.testing.screenshot.Screenshot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

/**
 * Base screenshot test that automatically renders and screenshots all Storybook stories.
 *
 * Extend this class in your app's androidTest directory:
 *
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class StoryScreenshotTest : BaseStoryScreenshotTest()
 * ```
 *
 * A single React surface is mounted for the entire test run. JS drives the story
 * loop — rendering each story and calling notifyStoryReady() after React commits.
 * The test thread screenshots and then resolves the JS Promise to advance the loop.
 * When all stories are done JS calls allStoriesDone() and the test exits.
 */
abstract class BaseStoryScreenshotTest {

    companion object {
        private const val TAG = "BaseStoryScreenshotTest"
        private const val DEFAULT_LOAD_TIMEOUT_MS = 5000L

        private const val SCREEN_WIDTH_PX = 1080
        private const val SCREEN_HEIGHT_PX = 1920
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )

    /**
     * Override to customize the React Native component name for story rendering.
     * Default is "StoryRenderer".
     */
    open fun getMainComponentName(): String = "StoryRenderer"

    /**
     * Override to customize the per-story timeout.
     * Default is 5000ms.
     */
    open fun getLoadTimeoutMs(): Long = DEFAULT_LOAD_TIMEOUT_MS

    /**
     * Override to skip specific stories.
     * Return true to include the story, false to skip it.
     * Default includes all stories.
     */
    open fun shouldScreenshotStory(storyId: String): Boolean = true

    /**
     * Screenshots all Storybook stories.
     *
     * Mounts a single StoryRenderer surface. JS iterates through all stories,
     * calling notifyStoryReady() after each commit. The test thread screenshots
     * and resolves the Promise to let JS advance.
     */
    @Test
    fun screenshotAllStories() {
        mountSurface { view ->
            runStoryLoop(view)
        }
    }

    private fun runStoryLoop(view: View) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val failures = mutableListOf<String>()
        var successCount = 0

        while (true) {
            StorybookRegistry.prepareForNextStory()
            val storyId = StorybookRegistry.awaitStoryReady(getLoadTimeoutMs()) ?: break

            if (!shouldScreenshotStory(storyId)) {
                Log.d(TAG, "Skipping story: $storyId")
                StorybookRegistry.resolveCurrentStory()
                continue
            }

            Log.d(TAG, "Screenshotting: $storyId")
            try {
                // Wait for Fabric to apply native mutations before snapping.
                waitTwoFrames()
                val screenshotName = storyId.replace("--", "_")
                instrumentation.runOnMainSync {
                    setLayerTypeSoftwareRecursively(view)
                    Screenshot.snap(view).setName(screenshotName).record()
                }
                Log.d(TAG, "Screenshot captured: $screenshotName")
                successCount++
            } catch (e: Exception) {
                failures.add("$storyId: ${e.message}")
                Log.e(TAG, "Failed to screenshot story: $storyId", e)
            } finally {
                StorybookRegistry.resolveCurrentStory()
            }
        }

        Log.d(TAG, "Screenshot results: $successCount passed, ${failures.size} failed")
        if (failures.isNotEmpty()) {
            Log.e(TAG, "Failed stories:\n${failures.joinToString("\n")}")
        }

        assertTrue("No stories were screenshotted", successCount > 0)
        assertTrue(
            "Some stories failed to screenshot: ${failures.joinToString(", ")}",
            failures.isEmpty()
        )
    }

    /**
     * Mounts the StoryRenderer surface, calls [onMounted] with the view, then tears down.
     * Handles both new arch (ReactHost/ReactSurface) and old arch (ReactRootView).
     */
    private fun mountSurface(onMounted: (view: View) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ReactApplication

        val reactHost = app.reactHost
        if (reactHost != null) {
            // New arch (Fabric/bridgeless): ReactHost + ReactSurface.
            // Fabric won't commit its render tree until the surface's host view is parented
            // to a real Window. Test processes don't have an Activity window, so we attach
            // via WindowManager using TYPE_APPLICATION_OVERLAY (requires SYSTEM_ALERT_WINDOW).
            // Wrap with the app theme so AppCompat widgets (e.g. Switch) resolve styled attrs.
            val context = ContextThemeWrapper(
                instrumentation.targetContext,
                instrumentation.targetContext.applicationInfo.theme
            )
            val surface = reactHost.createSurface(
                context,
                getMainComponentName(),
                Bundle()
            )

            val view = surface.view
                ?: throw IllegalStateException("ReactSurface returned a null view")

            val wm = instrumentation.targetContext
                .getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                SCREEN_WIDTH_PX,
                SCREEN_HEIGHT_PX,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            instrumentation.runOnMainSync {
                // Force software rendering so Screenshot.snap() can capture via draw(canvas).
                // WindowManager views are hardware-accelerated by default; GPU content is
                // invisible to a software canvas.
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                wm.addView(view, params)
                surface.start()
            }

            onMounted(view)

            instrumentation.runOnMainSync {
                surface.stop()
                wm.removeView(view)
            }
        } else {
            // Old arch: ReactRootView + ReactInstanceManager (deprecated API).
            val context = instrumentation.targetContext
            val rootView = ReactRootView(context)

            @Suppress("DEPRECATION")
            val reactInstanceManager = app.reactNativeHost.reactInstanceManager

            // ReactRootView.startReactApplication() checks isOnUiThread() internally.
            instrumentation.runOnMainSync {
                rootView.startReactApplication(reactInstanceManager, getMainComponentName(), Bundle())
            }

            // setupView().layout() calls measure()+layout() at the fixed dimensions, which
            // triggers onMeasure() → attachToReactInstanceManager() on the ReactRootView.
            ViewHelpers.setupView(rootView)
                .setExactWidthPx(SCREEN_WIDTH_PX)
                .setExactHeightPx(SCREEN_HEIGHT_PX)
                .layout()

            onMounted(rootView)

            instrumentation.runOnMainSync { rootView.unmountReactApplication() }
        }
    }

    /**
     * Recursively sets LAYER_TYPE_SOFTWARE on a view and all its descendants.
     *
     * view.draw(canvas) cannot capture children that have hardware display lists.
     * Fabric child views in a hardware-accelerated WindowManager window get hardware
     * display lists by default, so they render blank into a software canvas.
     * Walking the tree and forcing software layers ensures draw() sees all content.
     */
    private fun setLayerTypeSoftwareRecursively(view: View) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setLayerTypeSoftwareRecursively(view.getChildAt(i))
            }
        }
    }

    /**
     * Waits for two Choreographer frames on the main thread.
     *
     * After useEffect fires (React commit), Fabric still needs to apply its
     * native mutations in the next frame(s). Waiting two frames ensures the
     * shadow tree is fully flushed to native views before we screenshot.
     */
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
