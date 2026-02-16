package com.rnstorybookautoscreenshots

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.facebook.testing.screenshot.ScreenshotRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

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
     * Strip non-essential PNG chunks (tIME, tEXt, iTXt, zTXt) from all screenshots.
     * These chunks contain metadata like timestamps that cause false positives
     * in screenshot diffing tools. This operates at the byte level to preserve
     * the exact pixel data without re-encoding.
     */
    private fun stripPngMetadata() {
        val screenshotDir = File(
            System.getProperty("com.facebook.testing.screenshot.album") ?: return
        )
        screenshotDir.listFiles()?.filter { it.extension == "png" }?.forEach { file ->
            try {
                stripPngChunks(file)
            } catch (e: Exception) {
                // Don't fail the test run over metadata stripping
            }
        }
    }

    private val METADATA_CHUNKS = setOf("tIME", "tEXt", "iTXt", "zTXt", "pHYs", "gAMA", "cHRM", "sRGB", "iCCP")
    private val PNG_SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10) // \x89PNG\r\n\x1a\n

    private fun stripPngChunks(file: File) {
        val data = file.readBytes()
        if (data.size < 8 || !data.sliceArray(0..7).contentEquals(PNG_SIGNATURE)) return

        val output = ByteArrayOutputStream()
        output.write(PNG_SIGNATURE)

        var offset = 8
        while (offset + 8 <= data.size) {
            val length = ByteBuffer.wrap(data, offset, 4).int
            val chunkType = String(data, offset + 4, 4)
            val totalChunkSize = 4 + 4 + length + 4 // length + type + data + crc

            if (offset + totalChunkSize > data.size) break

            if (chunkType !in METADATA_CHUNKS) {
                output.write(data, offset, totalChunkSize)
            }

            offset += totalChunkSize
        }

        file.writeBytes(output.toByteArray())
    }
}
