package com.rnstorybookautoscreenshots

import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

/**
 * Base activity for rendering individual stories in screenshot tests.
 *
 * Extend this class in your app and register it in AndroidManifest.xml:
 *
 * ```kotlin
 * class StoryRendererActivity : BaseStoryRendererActivity()
 * ```
 *
 * Make sure to register "StoryRenderer" as a React Native component in your app.
 */
open class BaseStoryRendererActivity : ReactActivity() {

    companion object {
        const val EXTRA_STORY_NAME = "storyName"
        const val DEFAULT_STORY = "MyFeature/Initial"
    }

    /**
     * Returns the name of the component registered for story rendering.
     * Override this if you registered your StoryRenderer component with a different name.
     */
    override fun getMainComponentName(): String = "StoryRenderer"

    /**
     * Returns the instance of the ReactActivityDelegate with custom launch options.
     */
    override fun createReactActivityDelegate(): ReactActivityDelegate {
        return object : DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled) {
            override fun getLaunchOptions(): Bundle? {
                val bundle = Bundle()
                val storyName = intent.getStringExtra(EXTRA_STORY_NAME) ?: DEFAULT_STORY
                bundle.putString("storyName", storyName)
                return bundle
            }
        }
    }
}
