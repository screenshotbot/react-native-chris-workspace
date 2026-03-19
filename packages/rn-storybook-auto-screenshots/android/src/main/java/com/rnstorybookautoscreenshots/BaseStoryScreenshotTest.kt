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
 * This test automatically bootstraps the story manifest if it doesn't exist,
 * then creates a screenshot for each story. No manual test methods needed -
 * just add stories to Storybook and they get tested automatically.
 */
abstract class BaseStoryScreenshotTest {

    companion object {
        private const val TAG = "BaseStoryScreenshotTest"
        private const val DEFAULT_LOAD_TIMEOUT_MS = 5000L
        private const val DEFAULT_BOOTSTRAP_TIMEOUT_MS = 10000L
        private const val DEFAULT_CHOREOGRAPHER_FRAMES = 3
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
     * Override to customize how many Choreographer frames are awaited after
     * a story signals ready. Fabric schedules its native-view mount phase as
     * a Choreographer FrameCallback (VSYNC-driven), so waitForIdleSync() alone
     * cannot catch it. Each frame also drains follow-up Looper work before
     * the next frame is awaited.
     * Default is 3.
     */
    open fun getChoreographerFrames(): Int = DEFAULT_CHOREOGRAPHER_FRAMES

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
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // Disable system animations so native widgets (e.g. SwitchCompat) render
        // their final state immediately rather than mid-animation.
        disableAnimations(instrumentation)

        val context = instrumentation.targetContext
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

        for (story in stories) {
            try {
                screenshotStory(story)
                successCount++
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

    private fun screenshotStory(storyInfo: StoryInfo) {
        val storyName = storyInfo.toStoryName()
        Log.d(TAG, "Screenshotting: $storyName (id: ${storyInfo.id})")

        StorybookRegistry.prepareForNextStory()
        renderStory(storyName) { view ->
            StorybookRegistry.awaitStoryReady(getLoadTimeoutMs())
            // Fabric schedules its mount phase as a Choreographer FrameCallback, not a
            // Looper message, so waitForIdleSync() alone cannot catch it. We await N
            // frames here; each frame also drains its trailing Looper work before the
            // next frame is requested, covering multi-pass native widget layout too.
            awaitChoreographerFrames(
                InstrumentationRegistry.getInstrumentation(),
                getChoreographerFrames()
            )
            val screenshotName = storyInfo.id.replace("--", "_")
            Screenshot.snap(view).setName(screenshotName).record()
            Log.d(TAG, "Screenshot captured: $screenshotName")
        }
    }

    /**
     * Awaits [count] Choreographer frames on the main thread. After each frame,
     * drains the Looper to catch any follow-up work the frame may have posted.
     */
    private fun awaitChoreographerFrames(
        instrumentation: android.app.Instrumentation,
        count: Int
    ) {
        repeat(count) {
            val latch = CountDownLatch(1)
            instrumentation.runOnMainSync {
                Choreographer.getInstance().postFrameCallback { latch.countDown() }
            }
            check(latch.await(5, TimeUnit.SECONDS)) {
                "Timed out waiting for Choreographer frame"
            }
            instrumentation.waitForIdleSync()
        }
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
            // New arch (bridgeless): ReactHost + ReactSurface
            // Fabric requires the view to be attached to a real window before it will
            // commit its render tree, so we attach via WindowManager before starting.
            // Wrap with the app theme so AppCompat widgets (e.g. Switch) initialize correctly.
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
            )

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
            // Old arch: ReactRootView + ReactInstanceManager
            val context = instrumentation.targetContext
            val rootView = ReactRootView(context)

            @Suppress("DEPRECATION")
            val reactInstanceManager = app.reactNativeHost.reactInstanceManager

            // startReactApplication asserts UI thread
            instrumentation.runOnMainSync {
                rootView.startReactApplication(reactInstanceManager, getMainComponentName(), props)
            }

            // ViewHelpers.layout() triggers measure() → onMeasure() → attachToReactInstanceManager()
            ViewHelpers.setupView(rootView)
                .setExactWidthPx(SCREEN_WIDTH_PX)
                .setExactHeightPx(SCREEN_HEIGHT_PX)
                .layout()

            onRendered(rootView)

            instrumentation.runOnMainSync { rootView.unmountReactApplication() }
        }
    }

    /**
     * Disables system-wide animation scales via UiAutomation shell commands.
     * This prevents native widget animations (e.g. SwitchCompat thumb) from
     * rendering in an intermediate state when the screenshot is taken.
     */
    private fun disableAnimations(instrumentation: android.app.Instrumentation) {
        listOf(
            "animator_duration_scale",
            "transition_animation_scale",
            "window_animation_scale"
        ).forEach { key ->
            instrumentation.uiAutomation.executeShellCommand(
                "settings put global $key 0"
            ).close()
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
