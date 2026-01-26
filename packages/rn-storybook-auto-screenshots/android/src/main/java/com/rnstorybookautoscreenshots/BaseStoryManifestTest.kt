package com.rnstorybookautoscreenshots

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Base test that generates the story manifest.
 *
 * Extend this class in your app's androidTest directory:
 *
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class StoryManifestBootstrapTest : BaseStoryManifestTest() {
 *     override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java
 * }
 * ```
 *
 * Run this test first to create the manifest file that screenshot tests use
 * for automatic story discovery.
 */
abstract class BaseStoryManifestTest {

    companion object {
        private const val TAG = "BaseStoryManifestTest"
        private const val DEFAULT_LOAD_TIMEOUT_MS = 8000L
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
     * Override to customize the initial story used for bootstrapping.
     * Default is "MyFeature/Initial".
     */
    open fun getInitialStoryName(): String = "MyFeature/Initial"

    /**
     * Override to customize the React Native load timeout.
     * Default is 8000ms.
     */
    open fun getLoadTimeoutMs(): Long = DEFAULT_LOAD_TIMEOUT_MS

    /**
     * Launches the app to generate the story manifest.
     * After this test runs, screenshot tests can discover all stories.
     */
    @Test
    fun generateStoriesManifest() {
        Log.d(TAG, "Launching StoryRenderer to generate manifest...")

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            getStoryRendererActivityClass()
        ).apply {
            putExtra(BaseStoryRendererActivity.EXTRA_STORY_NAME, getInitialStoryName())
        }

        val scenario = ActivityScenario.launch<BaseStoryRendererActivity>(intent)

        // Wait for React Native to fully load and register stories
        Thread.sleep(getLoadTimeoutMs())

        scenario.close()

        // Verify manifest was created in external files directory
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val externalDir = context.getExternalFilesDir("screenshots")
        val manifestFile = File(externalDir, StorybookRegistry.STORIES_FILE_NAME)

        assertTrue(
            "Story manifest should be created at ${manifestFile.absolutePath}",
            manifestFile.exists()
        )

        val stories = StorybookRegistry.getStoriesFromFile(externalDir!!)
        Log.d(TAG, "Generated manifest with ${stories.size} stories:")
        stories.forEach { story ->
            Log.d(TAG, "  - ${story.title}/${story.name} (${story.id})")
        }

        assertTrue("Should find at least one story", stories.isNotEmpty())
    }
}
