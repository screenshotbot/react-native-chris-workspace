package com.rnstorybookautoscreenshots

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Native module with two responsibilities:
 * - Receives the story list from JS and writes it to disk for test discovery.
 * - Synchronises the test thread with JS rendering via a CountDownLatch/Promise handshake.
 *
 * Protocol per story:
 *   1. Test calls  prepareForNextStory()          — arms a fresh latch.
 *   2. JS   calls  notifyStoryReady(id, promise)  — stores promise, counts down latch.
 *   3. Test calls  awaitStoryReady(timeout)        — blocks until latch fires, returns story id.
 *   4. Test screenshots, then calls resolveCurrentStory() — resolves the JS promise.
 *   5. JS advances to the next story (repeat from 1), or calls allStoriesDone().
 */
class StorybookRegistry(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "StorybookRegistry"
        const val STORIES_FILE_NAME = "storybook_stories.json"

        @Volatile private var storyReadyLatch: CountDownLatch? = null
        @Volatile private var pendingStoryId: String? = null
        @Volatile private var pendingPromise: Promise? = null
        @Volatile private var isDone = false

        /**
         * Reset state and arm a fresh latch. Call before each story.
         */
        fun prepareForNextStory() {
            storyReadyLatch = CountDownLatch(1)
        }

        /**
         * Blocks until JS calls notifyStoryReady (or allStoriesDone), or the timeout elapses.
         * Returns the story id, or null if all stories are done or the timeout elapsed.
         */
        fun awaitStoryReady(timeoutMs: Long): String? {
            storyReadyLatch?.await(timeoutMs, TimeUnit.MILLISECONDS)
            return if (isDone) null else pendingStoryId
        }

        /**
         * Resolves the Promise that JS is awaiting, letting it advance to the next story.
         * Call this after the screenshot has been captured.
         */
        fun resolveCurrentStory() {
            pendingPromise?.resolve(null)
            pendingPromise = null
            pendingStoryId = null
        }

        /**
         * Read stories from the manifest file.
         * Used by screenshot tests to get the list of all stories.
         */
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
     * Called from JS after React commits a story render.
     * Stores the promise (resolved later by resolveCurrentStory) and unblocks the test thread.
     */
    @ReactMethod
    fun notifyStoryReady(storyId: String, promise: Promise) {
        pendingStoryId = storyId
        pendingPromise = promise
        storyReadyLatch?.countDown()
    }

    /**
     * Called from JS when all stories have been rendered.
     * Unblocks awaitStoryReady so the test loop can exit.
     */
    @ReactMethod
    fun allStoriesDone() {
        isDone = true
        storyReadyLatch?.countDown()
    }

    /**
     * Called from JS to register the list of available stories.
     * Writes to external files directory for test access.
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

            // Write to external files dir (works on all Android versions without permissions)
            val externalDir = reactApplicationContext.getExternalFilesDir("screenshots")
            if (externalDir != null) {
                externalDir.mkdirs()
                val externalFile = File(externalDir, STORIES_FILE_NAME)
                externalFile.writeText(jsonString)
                Log.d(TAG, "Wrote ${stories.length()} stories to ${externalFile.absolutePath}")
            }

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
     * Convert to format used by the StoryRenderer component: "Title/Name"
     */
    fun toStoryName(): String = "$title/$name"
}
