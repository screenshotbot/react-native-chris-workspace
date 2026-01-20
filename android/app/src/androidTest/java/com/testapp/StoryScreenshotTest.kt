package com.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rnstorybookautoscreenshots.BaseStoryRendererActivity
import com.rnstorybookautoscreenshots.BaseStoryScreenshotTest
import org.junit.runner.RunWith

/**
 * Screenshot test that automatically discovers and tests all Storybook stories.
 * Extends the base class from rn-storybook-auto-screenshots.
 *
 * Usage:
 * 1. Run StoryManifestBootstrapTest first to generate the manifest
 * 2. Run this test to screenshot all discovered stories
 */
@RunWith(AndroidJUnit4::class)
class StoryScreenshotTest : BaseStoryScreenshotTest() {

    override fun getStoryRendererActivityClass(): Class<out BaseStoryRendererActivity> =
        StoryRendererActivity::class.java
}
