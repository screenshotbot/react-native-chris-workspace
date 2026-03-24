package com.rnstorybookautoscreenshots

import android.Manifest
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
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
        val externalDir = context.getExternalFilesDir("screenshots")
        val manifestFile = File(externalDir, StorybookRegistry.STORIES_FILE_NAME)

        if (!manifestFile.exists()) {
            Log.d(TAG, "Manifest not found, bootstrapping...")
            bootstrapManifest(manifestFile)
        }

        val allStories = StorybookRegistry.getStoriesFromFile(externalDir!!)
        val stories = allStories.filter { shouldScreenshotStory(it) }

        Log.d(TAG, "Found ${allStories.size} stories, ${stories.size} after filtering")
        assertTrue("No stories found in manifest", stories.isNotEmpty())

        var successCount = 0
        var failureCount = 0
        val failures = mutableListOf<String>()

        // Keep a single surface alive for all stories. The first story is passed as the
        // initial prop; subsequent stories are switched via loadStory() events so that
        // _idToPrepared is only built once and each switch is fast.
        StorybookRegistry.prepareForNextStory()
        renderStory(stories.first().toStoryName()) { view ->
            for (story in stories) {
                try {
                    if (story != stories.first()) {
                        StorybookRegistry.prepareForNextStory()
                        StorybookRegistry.loadStory(story.toStoryName())
                    }
                    StorybookRegistry.awaitStoryReady(getLoadTimeoutMs())
                    // Fabric dispatches view-tree mutations to the main thread asynchronously
                    // after React's JS-side commit. notifyStoryReady() fires in useEffect
                    // (post-commit, JS thread), but the main thread may not have applied those
                    // mutations yet. runOnMainSync flushes the main thread's pending queue so
                    // the screenshot captures the updated view rather than the first story.
                    InstrumentationRegistry.getInstrumentation().runOnMainSync { }
                    val screenshotName = story.id.replace("--", "_")
                    Screenshot.snap(view).setName(screenshotName).record()
                    Log.d(TAG, "Screenshot captured: $screenshotName")
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                    val errorMsg = "${story.title}/${story.name}: ${e.message}"
                    failures.add(errorMsg)
                    Log.e(TAG, "Failed to screenshot story: $errorMsg", e)
                }
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
        renderStory(BOOTSTRAP_STORY_NAME) {
            waitForManifestFile(manifestFile)
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

            onRendered(view)

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
                rootView.startReactApplication(reactInstanceManager, getMainComponentName(), props)
            }

            // setupView().layout() calls measure()+layout() at the fixed dimensions, which
            // triggers onMeasure() → attachToReactInstanceManager() on the ReactRootView.
            ViewHelpers.setupView(rootView)
                .setExactWidthPx(SCREEN_WIDTH_PX)
                .setExactHeightPx(SCREEN_HEIGHT_PX)
                .layout()

            onRendered(rootView)

            instrumentation.runOnMainSync { rootView.unmountReactApplication() }
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
