package com.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rnstorybookautoscreenshots.BaseStoryManifestTest
import com.rnstorybookautoscreenshots.BaseStoryRendererActivity
import org.junit.runner.RunWith

/**
 * Bootstrap test that generates the story manifest.
 * Extends the base class from rn-storybook-auto-screenshots.
 *
 * Run this test first to create the manifest file that StoryScreenshotTest uses
 * for automatic story discovery.
 */
@RunWith(AndroidJUnit4::class)
class StoryManifestBootstrapTest : BaseStoryManifestTest() {

    override fun getStoryRendererActivityClass(): Class<out BaseStoryRendererActivity> =
        StoryRendererActivity::class.java
}
