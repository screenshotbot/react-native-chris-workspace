package com.rnstorybookautoscreenshots

/**
 * Functional interface for filtering which stories should be screenshotted.
 */
fun interface StoryFilter {
    fun shouldInclude(storyInfo: StoryInfo): Boolean
}

/**
 * Built-in story filters.
 */
object StoryFilters {

    /**
     * Matches all stories.
     */
    fun all(): StoryFilter = StoryFilter { true }

    /**
     * Matches no stories.
     */
    fun none(): StoryFilter = StoryFilter { false }

    /**
     * Matches stories with the exact title.
     */
    fun title(title: String): StoryFilter = StoryFilter { story ->
        story.title.equals(title, ignoreCase = true)
    }

    /**
     * Matches stories with the exact name.
     */
    fun name(name: String): StoryFilter = StoryFilter { story ->
        story.name.equals(name, ignoreCase = true)
    }

    /**
     * Matches stories using a glob pattern against "Title/Name" format.
     * Supports * (any characters) and ? (single character).
     */
    fun glob(pattern: String): StoryFilter = StoryFilter { story ->
        matchesGlob(story.toStoryName(), pattern)
    }

    /**
     * Matches stories using a regex pattern against "Title/Name" format.
     */
    fun regex(pattern: Regex): StoryFilter = StoryFilter { story ->
        pattern.containsMatchIn(story.toStoryName())
    }

    /**
     * Matches stories using a regex pattern string against "Title/Name" format.
     */
    fun regex(pattern: String): StoryFilter = regex(Regex(pattern, RegexOption.IGNORE_CASE))

    /**
     * Matches stories where the title starts with the given prefix.
     */
    fun titleStartsWith(prefix: String): StoryFilter = StoryFilter { story ->
        story.title.startsWith(prefix, ignoreCase = true)
    }

    /**
     * Matches stories where the title contains the given substring.
     */
    fun titleContains(substring: String): StoryFilter = StoryFilter { story ->
        story.title.contains(substring, ignoreCase = true)
    }

    /**
     * Matches stories where the name contains the given substring.
     */
    fun nameContains(substring: String): StoryFilter = StoryFilter { story ->
        story.name.contains(substring, ignoreCase = true)
    }

    private fun matchesGlob(text: String, pattern: String): Boolean {
        val regex = buildString {
            append("^")
            for (char in pattern) {
                when (char) {
                    '*' -> append(".*")
                    '?' -> append(".")
                    '.', '(', ')', '[', ']', '{', '}', '^', '$', '|', '\\' -> {
                        append("\\")
                        append(char)
                    }
                    else -> append(char)
                }
            }
            append("$")
        }
        return text.matches(Regex(regex, RegexOption.IGNORE_CASE))
    }
}

/**
 * Combines two filters with AND logic.
 * Story must match both filters to be included.
 */
infix fun StoryFilter.and(other: StoryFilter): StoryFilter = StoryFilter { story ->
    this.shouldInclude(story) && other.shouldInclude(story)
}

/**
 * Combines two filters with OR logic.
 * Story must match at least one filter to be included.
 */
infix fun StoryFilter.or(other: StoryFilter): StoryFilter = StoryFilter { story ->
    this.shouldInclude(story) || other.shouldInclude(story)
}

/**
 * Negates a filter.
 * Story is included only if it does NOT match the original filter.
 */
fun StoryFilter.not(): StoryFilter = StoryFilter { story ->
    !this.shouldInclude(story)
}
