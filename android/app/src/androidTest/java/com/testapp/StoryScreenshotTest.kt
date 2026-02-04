package com.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rnstorybookautoscreenshots.BaseStoryScreenshotTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryScreenshotTest : BaseStoryScreenshotTest() {
    override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java
}
