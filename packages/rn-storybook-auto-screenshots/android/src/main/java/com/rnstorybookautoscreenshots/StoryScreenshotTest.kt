package com.rnstorybookautoscreenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

/**
 * Runs screenshot tests for all Storybook stories automatically.
 *
 * This class is provided by the library — no setup required beyond
 * extending BaseStoryRendererActivity in your app.
 */
@RunWith(AndroidJUnit4::class)
class StoryScreenshotTest : BaseStoryScreenshotTest() {
    override fun getStoryRendererActivityClass() = BaseStoryRendererActivity::class.java
}
