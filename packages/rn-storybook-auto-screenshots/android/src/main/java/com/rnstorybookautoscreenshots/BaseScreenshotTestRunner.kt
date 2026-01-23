package com.rnstorybookautoscreenshots

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.facebook.testing.screenshot.ScreenshotRunner
import java.io.File

/**
 * Base test runner for screenshot tests.
 *
 * Extend this class in your app:
 *
 * ```kotlin
 * class ScreenshotTestRunner : BaseScreenshotTestRunner()
 * ```
 *
 * Then configure in your app's build.gradle:
 *
 * ```gradle
 * android {
 *     defaultConfig {
 *         testInstrumentationRunner "com.yourapp.ScreenshotTestRunner"
 *     }
 * }
 * ```
 */
open class BaseScreenshotTestRunner : AndroidJUnitRunner() {

    override fun onCreate(args: Bundle) {
        // Configure screenshot directory to use app-specific storage
        val screenshotDir = File(targetContext.filesDir, "screenshots")
        screenshotDir.mkdirs()

        // Set the directory as a system property for the screenshot library
        System.setProperty("com.facebook.testing.screenshot.album", screenshotDir.absolutePath)

        ScreenshotRunner.onCreate(this, args)
        super.onCreate(args)
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        ScreenshotRunner.onDestroy()
        super.finish(resultCode, results)
    }
}
