package com.rnstorybookautoscreenshots

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StorybookRegistryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // -- StoryInfo.toStoryName() --

    @Test
    fun toStoryName_formatsAsTitleSlashName() {
        val info = StoryInfo(id = "myfeature--initial", title = "MyFeature", name = "Initial")
        assertEquals("MyFeature/Initial", info.toStoryName())
    }

    @Test
    fun toStoryName_handlesEmptyStrings() {
        val info = StoryInfo(id = "", title = "", name = "")
        assertEquals("/", info.toStoryName())
    }

    // -- getStoriesFromFile() --

    @Test
    fun getStoriesFromFile_returnsStoriesFromValidJson() {
        val stories = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "myfeature--initial")
                put("title", "MyFeature")
                put("name", "Initial")
            })
            put(JSONObject().apply {
                put("id", "myfeature--with-clicks")
                put("title", "MyFeature")
                put("name", "With Clicks")
            })
        }
        val manifest = JSONObject().apply {
            put("stories", stories)
            put("generatedAt", 1234567890)
        }

        val dir = tempFolder.root
        java.io.File(dir, StorybookRegistry.STORIES_FILE_NAME).writeText(manifest.toString())

        val result = StorybookRegistry.getStoriesFromFile(dir)

        assertEquals(2, result.size)
        assertEquals("myfeature--initial", result[0].id)
        assertEquals("MyFeature", result[0].title)
        assertEquals("Initial", result[0].name)
        assertEquals("myfeature--with-clicks", result[1].id)
        assertEquals("With Clicks", result[1].name)
    }

    @Test
    fun getStoriesFromFile_returnsEmptyListWhenFileDoesNotExist() {
        val dir = tempFolder.root
        val result = StorybookRegistry.getStoriesFromFile(dir)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getStoriesFromFile_returnsEmptyListForMalformedJson() {
        val dir = tempFolder.root
        java.io.File(dir, StorybookRegistry.STORIES_FILE_NAME).writeText("not valid json")

        val result = StorybookRegistry.getStoriesFromFile(dir)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getStoriesFromFile_returnsEmptyListForEmptyStoriesArray() {
        val manifest = JSONObject().apply {
            put("stories", JSONArray())
            put("generatedAt", 1234567890)
        }

        val dir = tempFolder.root
        java.io.File(dir, StorybookRegistry.STORIES_FILE_NAME).writeText(manifest.toString())

        val result = StorybookRegistry.getStoriesFromFile(dir)
        assertTrue(result.isEmpty())
    }
}
