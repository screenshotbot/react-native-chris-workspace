package com.rnstorybookautoscreenshots

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.facebook.testing.screenshot.Screenshot
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
        private const val DEFAULT_LOAD_TIMEOUT_MS = 5000L
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

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            getStoryRendererActivityClass()
        ).apply {
            putExtra(BaseStoryRendererActivity.EXTRA_STORY_NAME, storyName)
        }

        val scenario = ActivityScenario.launch<BaseStoryRendererActivity>(intent)

        // Wait for React Native to load and render
        Thread.sleep(getLoadTimeoutMs())

        scenario.onActivity { activity ->
            val rootView = activity.window.decorView.rootView

            // Use story ID as screenshot name (replace -- with _ for filesystem compatibility)
            val screenshotName = storyInfo.id.replace("--", "_")

            // Capture screenshot using screenshot-tests-for-android
            // In record mode: saves baseline images
            // In verify mode: compares against baselines
            Screenshot.snap(rootView)
                .setName(screenshotName)
                .record()

            Log.d(TAG, "Screenshot captured: $screenshotName")
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
