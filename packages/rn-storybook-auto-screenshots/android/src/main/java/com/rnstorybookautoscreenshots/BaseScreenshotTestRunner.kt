package com.rnstorybookautoscreenshots

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.facebook.testing.screenshot.ScreenshotRunner
import java.io.File
import java.io.FileOutputStream

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
        stripPngMetadata()
        ScreenshotRunner.onDestroy()
        super.finish(resultCode, results)
    }

    /**
     * Re-encode all PNG screenshots to strip EXIF metadata (timestamps, etc.)
     * that causes false positives in screenshot diffing tools.
     */
    private fun stripPngMetadata() {
        val screenshotDir = File(
            System.getProperty("com.facebook.testing.screenshot.album") ?: return
        )
        screenshotDir.listFiles()?.filter { it.extension == "png" }?.forEach { file ->
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEach
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
            } catch (e: Exception) {
                // Don't fail the test run over metadata stripping
            }
        }
    }
}
