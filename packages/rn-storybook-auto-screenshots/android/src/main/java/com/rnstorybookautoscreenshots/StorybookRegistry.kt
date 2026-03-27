package com.rnstorybookautoscreenshots

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Native module with three responsibilities:
 * - Receives the story list from JS and writes it to disk for test discovery.
 * - Provides awaitNextStory(), a Promise-based pull that lets JS wait for the
 *   next story ID from the test runner rather than receiving events.
 * - Synchronises the test thread with JS rendering via a CountDownLatch.
 *
 * Communication flow:
 *   Test thread → pushStory(id) → storyQueue
 *   JS thread   ← await awaitNextStory() ← background thread ← storyQueue
 *   JS thread   → notifyStoryReady()
 *   Test thread ← awaitStoryReady() ← storyReadyLatch
 */
class StorybookRegistry(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "StorybookRegistry"
        const val STORIES_FILE_NAME = "storybook_stories.json"

        // Capacity 1 so pushStory() blocks until JS has consumed the current entry,
        // giving natural back-pressure between the test runner and JS.
        private val storyQueue = LinkedBlockingQueue<String?>(1)

        @Volatile private var storyReadyLatch: CountDownLatch? = null

        /**
         * Called by the test thread to enqueue the next story for JS to render.
         * Pass null to signal that all stories are done.
         *
         * Blocks until JS has consumed the previous entry (queue capacity = 1).
         */
        fun pushStory(storyId: String?) {
            storyQueue.put(storyId)
        }

        /**
         * Call before rendering each story. Creates a fresh latch for [awaitStoryReady].
         */
        fun prepareForNextStory() {
            storyReadyLatch = CountDownLatch(1)
        }

        /**
         * Blocks until JS signals the story is rendered, or the timeout elapses.
         */
        fun awaitStoryReady(timeoutMs: Long) {
            storyReadyLatch?.await(timeoutMs, TimeUnit.MILLISECONDS)
        }

        /**
         * Read stories from the manifest file.
         * Used by screenshot tests to get list of all stories.
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
     * Promise-based pull — resolves with the next story ID once the test runner
     * pushes one via [pushStory], or null when the test runner signals done.
     *
     * The blocking queue.poll() runs on a background thread so the React Native
     * JS thread is never blocked (replaces the deprecated isBlockingSynchronousMethod).
     */
    @ReactMethod
    fun awaitNextStory(promise: Promise) {
        Thread {
            val storyId = storyQueue.poll(30, TimeUnit.SECONDS)
            promise.resolve(storyId)
        }.start()
    }

    /**
     * Called from JS when a story has finished rendering (or errored).
     * Releases the latch that the test thread is waiting on.
     */
    @ReactMethod
    fun notifyStoryReady() {
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
