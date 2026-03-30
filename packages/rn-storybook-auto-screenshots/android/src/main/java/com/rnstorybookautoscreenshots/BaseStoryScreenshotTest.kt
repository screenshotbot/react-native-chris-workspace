package com.rnstorybookautoscreenshots

import android.Manifest
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.facebook.react.ReactApplication
import com.facebook.react.ReactRootView
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Base screenshot test that automatically discovers and tests all Storybook stories.
 *
 * Extend this class in your app's androidTest directory:
 *
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class StoryScreenshotTest : BaseStoryScreenshotTest()
 * ```
 *
 * Experiment: js-driven-story-loop
 * ---------------------------------
 * JS drives the entire story sequence. StoryRenderer renders all stories one at a
 * time in a for loop, calling notifyStoryReady(storyId) after each render and
 * waiting for the Promise to resolve before moving to the next story.
 *
 * The test thread loops reactively: wait for notifyStoryReady → take screenshot →
 * resolve the Promise → repeat until allStoriesDone().
 *
 * No events, no isBlockingSynchronousMethod, no manifest pre-loading step.
 */
abstract class BaseStoryScreenshotTest {

    companion object {
        private const val TAG = "BaseStoryScreenshotTest"
        private const val DEFAULT_LOAD_TIMEOUT_MS = 5000L
        private const val DEFAULT_TOTAL_TIMEOUT_MS = 300_000L // 5 minutes for all stories

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
     * Override to customize the per-story screenshot timeout.
     * Default is 5000ms.
     */
    open fun getLoadTimeoutMs(): Long = DEFAULT_LOAD_TIMEOUT_MS

    /**
     * Screenshots all stories. JS tells us which story to screenshot and when —
     * the test thread just reacts to notifyStoryReady() calls.
     */
    @Test
    fun screenshotAllStories() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        StorybookRegistry.prepareForRun()

        val failures = mutableListOf<String>()

        mountSurface { view ->
            // React to stories as JS renders them, until allStoriesDone() is called.
            while (true) {
                StorybookRegistry.prepareForNextStory()
                val storyId = StorybookRegistry.awaitStoryReady(getLoadTimeoutMs())

                if (storyId == null) {
                    // allStoriesDone() was called — JS has finished.
                    Log.d(TAG, "All stories done")
                    break
                }

                try {
                    // Two frames so Fabric's native view mutations are fully applied.
                    waitTwoFrames()

                    val screenshotName = storyId.replace("--", "_")
                    instrumentation.runOnMainSync {
                        ViewHelpers.setupView(view)
                            .setExactWidthPx(SCREEN_WIDTH_PX)
                            .setExactHeightPx(SCREEN_HEIGHT_PX)
                            .layout()
                        Screenshot.snap(view).setName(screenshotName).record()
                    }
                    Log.d(TAG, "Screenshot captured: $screenshotName")
                } catch (e: Exception) {
                    failures.add("$storyId: ${e.message}")
                    Log.e(TAG, "Failed to screenshot story: $storyId", e)
                } finally {
                    // Resolve the notifyStoryReady() Promise so JS can render the next story.
                    StorybookRegistry.resolveCurrentStory()
                }
            }
        }

        Log.d(TAG, "${failures.size} stories failed")
        assertTrue(
            "Some stories failed to screenshot: ${failures.joinToString(", ")}",
            failures.isEmpty()
        )
    }

    /**
     * Mounts a single React surface for the whole test run, calls [onMounted] with
     * the view, then tears down. Handles both old arch (ReactRootView) and new arch
     * (ReactSurface). No props are passed — JS drives itself.
     */
    private fun mountSurface(onMounted: (view: View) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ReactApplication

        val reactHost = app.reactHost
        if (reactHost != null) {
            // New arch (Fabric/bridgeless): ReactHost + ReactSurface.
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
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                wm.addView(view, params)
                surface.start()
            }

            try {
                onMounted(view)
            } finally {
                instrumentation.runOnMainSync {
                    surface.stop()
                    wm.removeView(view)
                }
            }
        } else {
            // Old arch: ReactRootView + ReactInstanceManager (deprecated API).
            val context = instrumentation.targetContext
            val rootView = ReactRootView(context)

            @Suppress("DEPRECATION")
            val reactInstanceManager = app.reactNativeHost.reactInstanceManager

            instrumentation.runOnMainSync {
                rootView.startReactApplication(reactInstanceManager, getMainComponentName(), Bundle())
            }

            ViewHelpers.setupView(rootView)
                .setExactWidthPx(SCREEN_WIDTH_PX)
                .setExactHeightPx(SCREEN_HEIGHT_PX)
                .layout()

            try {
                onMounted(rootView)
            } finally {
                instrumentation.runOnMainSync { rootView.unmountReactApplication() }
            }
        }
    }

    private fun waitTwoFrames() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(2) {
            val latch = CountDownLatch(1)
            instrumentation.runOnMainSync {
                Choreographer.getInstance().postFrameCallback { latch.countDown() }
            }
            latch.await(1000, TimeUnit.MILLISECONDS)
        }
    }
}
