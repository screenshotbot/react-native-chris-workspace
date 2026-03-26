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
 * This test mounts a single surface once, bootstraps the story manifest, then
 * drives each story via loadStory() events rather than remounting per story.
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
     *
     * Mounts a single ReactSurface for the entire run. The bootstrap render
     * (storyName="__bootstrap__") triggers registerStoriesWithNative() and
     * createPreparedStoryMapping() on the JS side. Subsequent stories are loaded
     * via loadStory() events fired on the main thread, each waiting on a fresh
     * CountDownLatch that notifyStoryReady() releases.
     */
    @Test
    fun screenshotAllStories() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val externalDir = instrumentation.targetContext.getExternalFilesDir("screenshots")!!
        val manifestFile = File(externalDir, StorybookRegistry.STORIES_FILE_NAME)

        // Prepare a latch for the bootstrap render.
        StorybookRegistry.prepareForNextStory()

        // Mount a single surface for the whole test. The bootstrap render will:
        //   1. Call registerStoriesWithNative() → write the manifest to disk.
        //   2. Call createPreparedStoryMapping() → populate _idToPrepared.
        //   3. Call notifyStoryReady() (via error path — __bootstrap__ is not a real story).
        renderStory(BOOTSTRAP_STORY_NAME) { view ->
            waitForManifestFile(manifestFile)
            StorybookRegistry.awaitStoryReady(getBootstrapTimeoutMs())
            Log.d(TAG, "Bootstrap complete, surface ready")

            val allStories = StorybookRegistry.getStoriesFromFile(externalDir)
            val stories = allStories.filter { shouldScreenshotStory(it) }
            Log.d(TAG, "Found ${allStories.size} stories, ${stories.size} after filtering")
            assertTrue("No stories found in manifest", stories.isNotEmpty())

            var successCount = 0
            val failures = mutableListOf<String>()

            for (story in stories) {
                try {
                    StorybookRegistry.prepareForNextStory()
                    instrumentation.runOnMainSync {
                        StorybookRegistry.loadStory(story.id)
                    }
                    StorybookRegistry.awaitStoryReady(getLoadTimeoutMs())

                    // Wait two frames so Fabric's native view mutations are fully applied
                    // before we snap the software-layer bitmap.
                    repeat(2) {
                        val frameLatch = CountDownLatch(1)
                        instrumentation.runOnMainSync {
                            Choreographer.getInstance().postFrameCallback { frameLatch.countDown() }
                        }
                        frameLatch.await(1000, TimeUnit.MILLISECONDS)
                    }

                    val screenshotName = story.id.replace("--", "_")
                    instrumentation.runOnMainSync {
                        Screenshot.snap(view).setName(screenshotName).record()
                    }
                    Log.d(TAG, "Screenshot captured: $screenshotName")
                    successCount++
                } catch (e: Exception) {
                    failures.add("${story.title}/${story.name}: ${e.message}")
                    Log.e(TAG, "Failed to screenshot story: ${story.id}", e)
                }
            }

            Log.d(TAG, "Screenshot results: $successCount passed, ${failures.size} failed")
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
                alpha = 0f
            }

            instrumentation.runOnMainSync {
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

            instrumentation.runOnMainSync {
                rootView.startReactApplication(reactInstanceManager, getMainComponentName(), props)
            }

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
