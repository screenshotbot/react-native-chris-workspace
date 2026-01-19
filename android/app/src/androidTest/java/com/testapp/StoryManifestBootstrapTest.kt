package com.testapp

import android.Manifest
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.rnstorybookautoscreenshots.BaseStoryRendererActivity
import com.rnstorybookautoscreenshots.StorybookRegistry

/**
 * Bootstrap test that generates the story manifest.
 *
 * Run this test first to create the manifest file that StoryScreenshotTest uses
 * for parameterized testing. This launches the StoryRenderer once, which triggers
 * the JS side to register all stories with the native module.
 */
@RunWith(AndroidJUnit4::class)
class StoryManifestBootstrapTest {

    companion object {
        private const val TAG = "StoryManifestBootstrap"
        private const val REACT_NATIVE_LOAD_TIMEOUT_MS = 8000L
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    /**
     * Launches the app to generate the story manifest.
     * After this test runs, StoryScreenshotTest can discover all stories.
     */
    @Test
    fun generateStoriesManifest() {
        Log.d(TAG, "Launching StoryRenderer to generate manifest...")

        // Launch with any story - this triggers story registration
        val intent = android.content.Intent(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            StoryRendererActivity::class.java
        ).apply {
            putExtra(BaseStoryRendererActivity.EXTRA_STORY_NAME, "MyFeature/Initial")
        }

        val scenario = ActivityScenario.launch<StoryRendererActivity>(intent)

        // Wait for React Native to fully load and register stories
        Thread.sleep(REACT_NATIVE_LOAD_TIMEOUT_MS)

        scenario.close()

        // Verify manifest was created in external files directory
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
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
