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
import java.io.File
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
 * Experiment: sync-story-blocking
 * --------------------------------
 * Instead of emitting loadStory events, the test thread pushes story IDs into a
 * LinkedBlockingQueue. JS pulls them via the blocking synchronous awaitNextStory()
 * call, which blocks the JS thread while native takes the screenshot. This inverts
 * the control flow: JS pulls from native rather than native pushing to JS.
 *
 * A single React surface is mounted for the entire run. The JS component loops:
 *   awaitNextStory() → render → notifyStoryReady() → awaitNextStory() → …
 *
 * The test thread drives the loop by pushing story IDs, waiting for notifyStoryReady,
 * taking a screenshot, then pushing the next ID. A null push signals JS to stop.
 */
abstract class BaseStoryScreenshotTest {

    companion object {
        private const val TAG = "BaseStoryScreenshotTest"
        private const val DEFAULT_LOAD_TIMEOUT_MS = 5000L
        private const val DEFAULT_BOOTSTRAP_TIMEOUT_MS = 10000L
        private const val BOOTSTRAP_STORY_ID = "__bootstrap__"

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
     * Override to customize the React Native load timeout per story.
     * Default is 5000ms.
     */
    open fun getLoadTimeoutMs(): Long = DEFAULT_LOAD_TIMEOUT_MS

    /**
     * Override to customize the timeout for manifest bootstrap.
     * Default is 10000ms.
     */
    open fun getBootstrapTimeoutMs(): Long = DEFAULT_BOOTSTRAP_TIMEOUT_MS

    /**
     * Override to filter which stories should be screenshotted.
     * Return true to include the story, false to skip it.
     * Default includes all stories.
     */
    open fun shouldScreenshotStory(storyInfo: StoryInfo): Boolean = true

    /**
     * Screenshots all stories found in the manifest.
     *
     * Mounts a single React surface for the entire run. JS blocks itself via
     * awaitNextStory() between renders; the test thread drives the sequence by
     * pushing story IDs and waiting for notifyStoryReady() after each render.
     */
    @Test
    fun screenshotAllStories() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val externalDir = instrumentation.targetContext.getExternalFilesDir("screenshots")!!
        val manifestFile = File(externalDir, StorybookRegistry.STORIES_FILE_NAME)

        // Push the bootstrap marker before mounting so JS unblocks immediately on mount.
        StorybookRegistry.prepareForNextStory()
        StorybookRegistry.pushStory(BOOTSTRAP_STORY_ID)

        mountSurface { view ->
            // Wait for JS to register stories and write the manifest.
            waitForManifestFile(manifestFile)
            StorybookRegistry.awaitStoryReady(getBootstrapTimeoutMs())
            Log.d(TAG, "Bootstrap complete")

            val allStories = StorybookRegistry.getStoriesFromFile(externalDir)
            val stories = allStories.filter { shouldScreenshotStory(it) }
            Log.d(TAG, "Found ${allStories.size} stories, ${stories.size} after filtering")
            assertTrue("No stories found in manifest", stories.isNotEmpty())

            val failures = mutableListOf<String>()

            for (story in stories) {
                try {
                    // Push the next story ID — JS unblocks from awaitNextStory() and renders it.
                    StorybookRegistry.prepareForNextStory()
                    StorybookRegistry.pushStory(story.id)

                    // Wait for JS to signal the story is rendered.
                    StorybookRegistry.awaitStoryReady(getLoadTimeoutMs())

                    // Two frames so Fabric's native view mutations are fully applied.
                    waitTwoFrames()

                    val screenshotName = story.id.replace("--", "_")
                    instrumentation.runOnMainSync {
                        ViewHelpers.setupView(view)
                            .setExactWidthPx(SCREEN_WIDTH_PX)
                            .setExactHeightPx(SCREEN_HEIGHT_PX)
                            .layout()
                        Screenshot.snap(view).setName(screenshotName).record()
                    }
                    Log.d(TAG, "Screenshot captured: $screenshotName")
                } catch (e: Exception) {
                    failures.add("${story.title}/${story.name}: ${e.message}")
                    Log.e(TAG, "Failed to screenshot story: ${story.id}", e)
                }
            }

            // Signal JS that there are no more stories.
            StorybookRegistry.pushStory(null)

            Log.d(TAG, "Screenshot results: ${stories.size - failures.size} passed, ${failures.size} failed")
            if (failures.isNotEmpty()) {
                Log.e(TAG, "Failed stories:\n${failures.joinToString("\n")}")
            }
            assertTrue(
                "Some stories failed to screenshot: ${failures.joinToString(", ")}",
                failures.isEmpty()
            )
        }
    }

    /**
     * Mounts a single React surface for the whole test run, calls [onMounted] with
     * the view, then tears down. Handles both old arch (ReactRootView) and new arch
     * (ReactSurface). No storyName prop is passed — JS drives itself via awaitNextStory().
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

    private fun waitForManifestFile(manifestFile: File) {
        val deadline = System.currentTimeMillis() + getBootstrapTimeoutMs()
        while (!manifestFile.exists() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }
        if (!manifestFile.exists()) {
            throw IllegalStateException(
                "Manifest file did not appear within ${getBootstrapTimeoutMs()}ms. " +
                "Make sure configure(view) is called in your app and the StoryRenderer is registered."
            )
        }
    }
}
