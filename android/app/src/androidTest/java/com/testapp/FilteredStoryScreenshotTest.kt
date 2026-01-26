package com.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rnstorybookautoscreenshots.BaseStoryScreenshotTest
import com.rnstorybookautoscreenshots.StoryFilter
import com.rnstorybookautoscreenshots.StoryFilters
import com.rnstorybookautoscreenshots.and
import com.rnstorybookautoscreenshots.not
import com.rnstorybookautoscreenshots.or
import org.junit.runner.RunWith

/**
 * Example of using the flexible StoryFilter API for screenshot tests.
 * This test only screenshots MyFeature stories, excluding any with "Many" in the name.
 */
@RunWith(AndroidJUnit4::class)
class FilteredStoryScreenshotTest : BaseStoryScreenshotTest() {

    override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java

    override fun getStoryFilter(): StoryFilter =
        StoryFilters.title("MyFeature") and StoryFilters.nameContains("Many").not()

    override fun getLoadTimeoutMs() = 8000L
}
