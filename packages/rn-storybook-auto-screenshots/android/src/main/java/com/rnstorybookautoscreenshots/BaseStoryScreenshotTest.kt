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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.test.rule.GrantPermissionRule
import com.facebook.react.ReactApplication
import com.facebook.react.ReactRootView
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

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
 * This test automatically bootstraps the story manifest if it doesn't exist,
 * then creates a screenshot for each story. No manual test methods needed -
 * just add stories to Storybook and they get tested automatically.
 */
abstract class BaseStoryScreenshotTest {

    companion object {
        private const val TAG = "BaseStoryScreenshotTest"
        private const val DEFAULT_LOAD_TIMEOUT_MS = 5000L
        private const val DEFAULT_BOOTSTRAP_TIMEOUT_MS = 10000L
        private const val BOOTSTRAP_STORY_NAME = "__bootstrap__"

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
     * Each story gets its own screenshot named after its ID.
     * If the manifest doesn't exist, it will be bootstrapped automatically.
     */
    @Test
    fun screenshotAllStories() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val externalDir = context.getExternalFilesDir("screenshots")!!
        val manifestFile = File(externalDir, StorybookRegistry.STORIES_FILE_NAME)

        // Always run the warm-up surface before the story loop, even if the manifest
        // already exists on disk from a previous run. This guarantees:
        //   1. The manifest is fresh (reflects the current story list).
        //   2. _idToPrepared is fully populated before any story's timeout starts.
        // Without this, stories on the first run after a manifest exists would have
        // to wait for createPreparedStoryMapping() themselves, non-deterministically
        // exceeding the 5 s timeout.
        Log.d(TAG, "Warming up Storybook (registering stories + building prepared map)...")
        bootstrapManifest(manifestFile)
        Log.d(TAG, "Warm-up complete")

        val allStories = StorybookRegistry.getStoriesFromFile(externalDir)
        val stories = allStories.filter { shouldScreenshotStory(it) }

        Log.d(TAG, "Found ${allStories.size} stories, ${stories.size} after filtering")
        assertTrue("No stories found in manifest", stories.isNotEmpty())

        var successCount = 0
        var failureCount = 0
        val failures = mutableListOf<String>()

        // Mount a fresh surface for each story, passing the story ID as the initial prop.
        // This avoids relying on DeviceEventEmitter to switch stories (unreliable in
        // bridgeless/new-arch mode) and ensures each story renders from a clean state.
        // We pass the ID (e.g. "example-button--primary") rather than the title/name
        // path so that StoryRenderer can look it up directly in _idToPrepared without
        // any string conversion that would break hierarchical titles like "Example/Button".
        for (story in stories) {
            try {
                StorybookRegistry.prepareForNextStory()
                renderStory(story.id) { view ->
                    StorybookRegistry.awaitStoryReady(getLoadTimeoutMs())
                    // Fabric dispatches view-tree mutations via the Choreographer
                    // (postFrameCallback / DISPATCH_UI), not via a plain Handler post.
                    // Posting our own postFrameCallback after awaitStoryReady guarantees
                    // that Fabric's earlier-registered callback (queued during the React
                    // commit, before useEffect fired) runs first, leaving the native view
                    // hierarchy up-to-date when we draw the screenshot.
                    val frameLatch = CountDownLatch(1)
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        Choreographer.getInstance().postFrameCallback { frameLatch.countDown() }
                    }
                    frameLatch.await(1000, TimeUnit.MILLISECONDS)
                    val screenshotName = story.id.replace("--", "_")
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        Screenshot.snap(view).setName(screenshotName).record()
                    }
                    Log.d(TAG, "Screenshot captured: $screenshotName")
                    successCount++
                }
            } catch (e: Exception) {
                failureCount++
                val errorMsg = "${story.title}/${story.name}: ${e.message}"
                failures.add(errorMsg)
                Log.e(TAG, "Failed to screenshot story: $errorMsg", e)
            }
        }

        Log.d(TAG, "Screenshot results: $successCount passed, $failureCount failed")
        if (failures.isNotEmpty()) {
            Log.e(TAG, "Failed stories:\n${failures.joinToString("\n")}")
        }

        assertTrue(
            "Some stories failed to screenshot: ${failures.joinToString(", ")}",
            failures.isEmpty()
        )
    }

    private fun bootstrapManifest(manifestFile: File) {
        Log.d(TAG, "Launching StoryRenderer to generate manifest...")
        // prepareForNextStory() so awaitStoryReady() below has a latch to wait on.
        // JS calls notifyStoryReady() only after createPreparedStoryMapping() finishes,
        // so by the time we return here _idToPrepared is fully populated and every
        // story in the loop can skip the expensive async mapping call.
        StorybookRegistry.prepareForNextStory()
        renderStory(BOOTSTRAP_STORY_NAME) {
            waitForManifestFile(manifestFile)
            StorybookRegistry.awaitStoryReady(getBootstrapTimeoutMs())
        }
        Log.d(TAG, "Bootstrap complete")
    }

    /**
     * Renders the given story name into a view, calls [onRendered] with that view,
     * then tears down. Handles both old arch (ReactRootView) and new arch (ReactSurface).
     */
    private fun renderStory(storyName: String, onRendered: (view: View) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ReactApplication
        val props = Bundle().apply { putString("storyName", storyName) }

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
                props
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
            ).apply {
                // alpha=0 lets the compositor skip this window entirely while still
                // satisfying Fabric's requirement that the surface be attached to a Window.
                alpha = 0f
            }

            instrumentation.runOnMainSync {
                // Force software rendering so Screenshot.snap() can capture via draw(canvas).
                // WindowManager views are hardware-accelerated by default; GPU content is
                // invisible to a software canvas.
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                wm.addView(view, params)
                surface.start()
            }

            try {
                onRendered(view)
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

            // ReactRootView.startReactApplication() checks isOnUiThread() internally.
            instrumentation.runOnMainSync {
                rootView.startReactApplication(reactInstanceManager, getMainComponentName(), props)
            }

            // setupView().layout() calls measure()+layout() at the fixed dimensions, which
            // triggers onMeasure() → attachToReactInstanceManager() on the ReactRootView.
            ViewHelpers.setupView(rootView)
                .setExactWidthPx(SCREEN_WIDTH_PX)
                .setExactHeightPx(SCREEN_HEIGHT_PX)
                .layout()

            try {
                onRendered(rootView)
            } finally {
                instrumentation.runOnMainSync { rootView.unmountReactApplication() }
            }
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
