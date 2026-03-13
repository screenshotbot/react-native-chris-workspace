package com.rnstorybookautoscreenshots

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
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
     * Override to provide a custom StoryRendererActivity class.
     * Defaults to BaseStoryRendererActivity, which is registered in the package manifest.
     */
    open fun getStoryRendererActivityClass(): Class<out BaseStoryRendererActivity> =
        BaseStoryRendererActivity::class.java

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

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            getStoryRendererActivityClass()
        ).apply {
            putExtra(BaseStoryRendererActivity.EXTRA_STORY_NAME, BOOTSTRAP_STORY_NAME)
        }

        // Launch once — RN initializes and registers all stories
        StorybookRegistry.prepareForNextStory()
        val scenario = ActivityScenario.launch<BaseStoryRendererActivity>(intent)
        StorybookRegistry.awaitStoryReady(getBootstrapTimeoutMs())

        if (!manifestFile.exists()) {
            waitForManifestFile(manifestFile)
        }

        val allStories = StorybookRegistry.getStoriesFromFile(externalDir!!)
        val stories = allStories.filter { shouldScreenshotStory(it) }

        Log.d(TAG, "Found ${allStories.size} stories, ${stories.size} after filtering")
        assertTrue("No stories found in manifest", stories.isNotEmpty())

        val failures = mutableListOf<String>()

        for (story in stories) {
            try {
                val storyName = story.toStoryName()
                Log.d(TAG, "Screenshotting: $storyName (id: ${story.id})")

                StorybookRegistry.prepareForNextStory()
                scenario.onActivity { activity -> activity.loadStory(storyName) }
                StorybookRegistry.awaitStoryReady(getLoadTimeoutMs())
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()

                scenario.onActivity { activity ->
                    val screenshotName = story.id.replace("--", "_")
                    Screenshot.snap(activity.window.decorView.rootView)
                        .setName(screenshotName)
                        .record()
                    Log.d(TAG, "Screenshot captured: $screenshotName")
                }
            } catch (e: Exception) {
                val errorMsg = "${story.title}/${story.name}: ${e.message}"
                failures.add(errorMsg)
                Log.e(TAG, "Failed to screenshot story: $errorMsg", e)
            }
        }

        scenario.close()

        Log.d(TAG, "Screenshot results: ${stories.size - failures.size} passed, ${failures.size} failed")

        if (failures.isNotEmpty()) {
            Log.e(TAG, "Failed stories:\n${failures.joinToString("\n")}")
        }

        assertTrue(
            "Some stories failed to screenshot: ${failures.joinToString(", ")}",
            failures.isEmpty()
        )
    }

    /**
     * Polls for the manifest file until it appears or the timeout elapses.
     * The file is written by JS as soon as RN has loaded and registered all stories,
     * so its appearance is a direct signal that RN is ready.
     * Throws if the file has not appeared by the deadline.
     */
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
