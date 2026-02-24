package com.rnstorybookautoscreenshots

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
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
 * class StoryScreenshotTest : BaseStoryScreenshotTest() {
 *     override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java
 * }
 * ```
 *
 * This test automatically bootstraps the story manifest if it doesn't exist,
 * then creates a screenshot for each story. No manual test methods needed -
 * just add stories to Storybook and they get tested automatically.
 */
abstract class BaseStoryScreenshotTest {

    companion object {
        private const val TAG = "BaseStoryScreenshotTest"
        private const val DEFAULT_LOAD_TIMEOUT_MS = 10000L
        private const val DEFAULT_BOOTSTRAP_TIMEOUT_MS = 10000L

        // Not a real story — bootstrap just needs RN to load and register all stories.
        // The StoryRenderer registers stories before attempting to look up the story name,
        // so any string works here.
        private const val BOOTSTRAP_STORY_NAME = "__bootstrap__"
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    /**
     * Override this to provide your app's StoryRendererActivity class.
     */
    abstract fun getStoryRendererActivityClass(): Class<out BaseStoryRendererActivity>

    /**
     * Override to customize the React Native load timeout per story.
     * Default is 5000ms.
     */
    open fun getLoadTimeoutMs(): Long = DEFAULT_LOAD_TIMEOUT_MS

    /**
     * Override to customize the timeout for manifest bootstrap.
     * This is used when the manifest doesn't exist and needs to be generated.
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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val externalDir = context.getExternalFilesDir("screenshots")
        val manifestFile = File(externalDir, StorybookRegistry.STORIES_FILE_NAME)

        // Bootstrap manifest if it doesn't exist
        if (!manifestFile.exists()) {
            Log.d(TAG, "Manifest not found, bootstrapping...")
            bootstrapManifest()
        }

        assertTrue(
            "Stories manifest not found at ${manifestFile.absolutePath}. Bootstrap failed.",
            manifestFile.exists()
        )

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

        StorybookRegistry.resetContentHeight()

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            getStoryRendererActivityClass()
        ).apply {
            putExtra(BaseStoryRendererActivity.EXTRA_STORY_NAME, storyName)
        }

        val scenario = ActivityScenario.launch<BaseStoryRendererActivity>(intent)

        // Poll for React Native to render and report content height, up to the timeout
        val deadline = System.currentTimeMillis() + getLoadTimeoutMs()
        do {
            Thread.sleep(100)
        } while (StorybookRegistry.getContentHeightDp() < 0 && System.currentTimeMillis() < deadline)

        // Read height on test thread before posting to UI thread, to avoid racing with onLayout
        val capturedHeightDp = StorybookRegistry.getContentHeightDp()
        Log.d(TAG, "Content height after wait: ${capturedHeightDp}dp")

        scenario.onActivity { activity ->
            val decorView = activity.window.decorView

            // rootWindowInsets gives the actual system bar pixel heights
            val insets = decorView.rootWindowInsets
            val topInset = insets?.systemWindowInsetTop ?: 0
            val bottomInset = insets?.systemWindowInsetBottom ?: 0
            Log.d(TAG, "rootWindowInsets: top=$topInset, bottom=$bottomInset, screen=${decorView.width}x${decorView.height}")

            val density = activity.resources.displayMetrics.density

            val fullBitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
            decorView.draw(Canvas(fullBitmap))

            val fullContentHeight = fullBitmap.height - topInset - bottomInset
            val cropHeight = if (capturedHeightDp > 0) {
                (capturedHeightDp * density).toInt().coerceAtMost(fullContentHeight)
            } else {
                fullContentHeight
            }
            Log.d(TAG, "Cropping: contentHeightDp=$capturedHeightDp, cropHeight=${cropHeight}px")
            val cropped = Bitmap.createBitmap(fullBitmap, 0, topInset, fullBitmap.width, cropHeight)
            fullBitmap.recycle()
            val imageView = ImageView(activity)
            imageView.setImageBitmap(cropped)
            imageView.scaleType = ImageView.ScaleType.FIT_XY

            ViewHelpers.setupView(imageView)
                .setExactWidthDp((cropped.width / density).toInt())
                .setExactHeightDp((cropped.height / density).toInt())
                .layout()

            val screenshotName = storyInfo.id.replace("--", "_")
            Screenshot.snap(imageView)
                .setName(screenshotName)
                .record()

            cropped.recycle()
            Log.d(TAG, "Screenshot captured: $screenshotName (${cropped.width}x${cropped.height})")
        }

        scenario.close()
    }

    /**
     * Bootstraps the story manifest by launching StoryRendererActivity.
     * This allows React Native to initialize and register all stories.
     */
    private fun bootstrapManifest() {
        Log.d(TAG, "Launching StoryRenderer to generate manifest...")

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            getStoryRendererActivityClass()
        ).apply {
            putExtra(BaseStoryRendererActivity.EXTRA_STORY_NAME, BOOTSTRAP_STORY_NAME)
        }

        val scenario = ActivityScenario.launch<BaseStoryRendererActivity>(intent)

        // Wait for React Native to fully load and register stories
        Thread.sleep(getBootstrapTimeoutMs())

        scenario.close()

        Log.d(TAG, "Bootstrap complete")
    }
}
