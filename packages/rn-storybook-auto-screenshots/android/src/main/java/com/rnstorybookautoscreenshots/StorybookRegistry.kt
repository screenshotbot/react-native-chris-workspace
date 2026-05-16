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
 * Native module with three responsibilities:
 * - Receives the story list from JS and writes it to disk for test discovery.
 * - Receives notifyStoryReady(storyId) from JS and holds the Promise until the
 *   test thread has taken the screenshot and calls resolveCurrentStory().
 * - Receives allStoriesDone() from JS to signal the test thread to exit.
 *
 * Communication flow:
 *   JS thread   → notifyStoryReady(storyId, promise) → storyReadyLatch.countDown()
 *   Test thread ← awaitStoryReady() ← storyReadyLatch
 *   Test thread → takes screenshot → resolveCurrentStory() → promise.resolve()
 *   JS thread   ← promise resolves → renders next story
 *   ...
 *   JS thread   → allStoriesDone() → allDoneLatch.countDown()
 *   Test thread ← awaitAllDone() ← allDoneLatch
 */
class StorybookRegistry(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "StorybookRegistry"
        const val STORIES_FILE_NAME = "storybook_stories.json"

        @Volatile private var storyReadyLatch: CountDownLatch? = null
        @Volatile private var allDoneLatch: CountDownLatch? = null
        @Volatile private var pendingPromise: Promise? = null
        @Volatile private var pendingStoryId: String? = null
        @Volatile private var isDone = false

        /**
         * Call once before mounting the surface to set up the all-done latch.
         */
        fun prepareForRun() {
            allDoneLatch = CountDownLatch(1)
            isDone = false
        }

        /**
         * Call before each story to create a fresh latch for [awaitStoryReady].
         */
        fun prepareForNextStory() {
            storyReadyLatch = CountDownLatch(1)
            isDone = false
            pendingStoryId = null
        }

        /**
         * Blocks until JS calls notifyStoryReady() or allStoriesDone(), or the timeout elapses.
         * Returns the story ID, or null if allStoriesDone() was called (or timeout).
         */
        fun awaitStoryReady(timeoutMs: Long): String? {
            storyReadyLatch?.await(timeoutMs, TimeUnit.MILLISECONDS)
            return if (isDone) null else pendingStoryId
        }

        /**
         * Resolves the Promise that notifyStoryReady() is holding.
         * Call this after taking the screenshot to let JS proceed to the next story.
         */
        fun resolveCurrentStory() {
            pendingPromise?.resolve(null)
            pendingPromise = null
        }

        /**
         * Blocks until JS calls allStoriesDone(), or the timeout elapses.
         */
        fun awaitAllDone(timeoutMs: Long) {
            allDoneLatch?.await(timeoutMs, TimeUnit.MILLISECONDS)
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
     * Called from JS after a story has been rendered and committed to the view.
     * Stores the Promise (so the test thread can resolve it after the screenshot)
     * and releases [awaitStoryReady] so the test thread knows it can proceed.
     */
    @ReactMethod
    fun notifyStoryReady(storyId: String, promise: Promise) {
        pendingStoryId = storyId
        pendingPromise = promise
        storyReadyLatch?.countDown()
    }

    /**
     * Called from JS after all stories have been rendered.
     * Releases [awaitAllDone] so the test thread can exit and unmount the surface.
     */
    @ReactMethod
    fun allStoriesDone() {
        isDone = true
        // Release any thread waiting on awaitStoryReady (in case it's still waiting).
        storyReadyLatch?.countDown()
        allDoneLatch?.countDown()
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
