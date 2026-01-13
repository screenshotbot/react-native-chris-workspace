package com.testapp

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Native module that receives the story list from Storybook JS side.
 * Stories are written to a file that screenshot tests can read.
 */
class StorybookRegistry(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "StorybookRegistry"
        const val STORIES_FILE_NAME = "storybook_stories.json"

        /**
         * Read stories from the manifest file.
         * Used by parameterized tests to get list of all stories.
         */
        @JvmStatic
        fun getStoriesFromFile(storageDir: File): List<StoryInfo> {
            val file = File(storageDir, STORIES_FILE_NAME)
            if (!file.exists()) {
                Log.w(TAG, "Stories file not found: ${file.absolutePath}")
                return emptyList()
            }
            return try {
                val json = file.readText()
                val obj = JSONObject(json)
                val stories = obj.getJSONArray("stories")
                (0 until stories.length()).map { i ->
                    val story = stories.getJSONObject(i)
                    StoryInfo(
                        id = story.getString("id"),
                        title = story.getString("title"),
                        name = story.getString("name")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading stories file", e)
                emptyList()
            }
        }
    }

    override fun getName(): String = "StorybookRegistry"

    /**
     * Called from JS to register the list of available stories.
     * Writes to both internal storage and sdcard for test access.
     */
    @ReactMethod
    fun registerStories(storiesArray: ReadableArray) {
        try {
            val stories = JSONArray()
            for (i in 0 until storiesArray.size()) {
                val storyMap = storiesArray.getMap(i)
                val storyObj = JSONObject().apply {
                    put("id", storyMap?.getString("id") ?: "")
                    put("title", storyMap?.getString("title") ?: "")
                    put("name", storyMap?.getString("name") ?: "")
                }
                stories.put(storyObj)
            }

            val manifest = JSONObject().apply {
                put("stories", stories)
                put("generatedAt", System.currentTimeMillis())
            }

            val jsonString = manifest.toString(2)

            // Write to internal storage
            val internalFile = File(reactApplicationContext.filesDir, STORIES_FILE_NAME)
            internalFile.writeText(jsonString)
            Log.d(TAG, "Wrote ${stories.length()} stories to ${internalFile.absolutePath}")

            // Also write to sdcard for easy test access
            val sdcardDir = File("/sdcard/screenshots/com.testapp.test")
            sdcardDir.mkdirs()
            val sdcardFile = File(sdcardDir, STORIES_FILE_NAME)
            sdcardFile.writeText(jsonString)
            Log.d(TAG, "Wrote ${stories.length()} stories to ${sdcardFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error registering stories", e)
        }
    }
}

/**
 * Data class representing a Storybook story.
 */
data class StoryInfo(
    val id: String,
    val title: String,
    val name: String
) {
    /**
     * Convert to format used by StoryRendererActivity: "Title/Name"
     */
    fun toStoryName(): String = "$title/$name"
}
