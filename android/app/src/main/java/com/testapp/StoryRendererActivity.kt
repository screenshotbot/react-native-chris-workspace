package com.testapp

import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

/**
 * Activity for rendering individual stories in screenshot tests.
 * This launches the StoryRenderer React Native component instead of the main app.
 */
class StoryRendererActivity : ReactActivity() {

  companion object {
    const val EXTRA_STORY_NAME = "storyName"
  }

  /**
   * Returns the name of the component registered for story rendering.
   */
  override fun getMainComponentName(): String = "StoryRenderer"

  /**
   * Returns the instance of the ReactActivityDelegate with custom launch options.
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate {
    return object : DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled) {
      override fun getLaunchOptions(): Bundle? {
        val bundle = Bundle()
        val storyName = intent.getStringExtra(EXTRA_STORY_NAME) ?: "MyFeature/Initial"
        bundle.putString("storyName", storyName)
        return bundle
      }
    }
  }
}
