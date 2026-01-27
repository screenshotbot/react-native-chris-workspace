package com.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rnstorybookautoscreenshots.BaseStoryManifestTest
import org.junit.runner.RunWith

/**
 * Bootstrap test that generates the story manifest.
 * Run this test first before running FilteredStoryScreenshotTest.
 */
@RunWith(AndroidJUnit4::class)
class StoryManifestBootstrapTest : BaseStoryManifestTest() {

    override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java

    override fun getLoadTimeoutMs() = 15000L
}
