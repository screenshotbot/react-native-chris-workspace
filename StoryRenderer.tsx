import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, NativeModules } from 'react-native';
import { view } from './.rnstorybook/storybook.requires';

const { StorybookRegistry } = NativeModules;

type StoryRendererProps = {
  storyName?: string;
};

/**
 * Converts a story name like "MyFeature/Initial" to Storybook's ID format "myfeature--initial"
 */
function storyNameToId(storyName: string): string {
  const [title, name] = storyName.split('/');
  // Storybook uses kebab-case and -- separator
  const titleKebab = title.toLowerCase().replace(/\s+/g, '-');
  const nameKebab = name.toLowerCase().replace(/\s+/g, '-');
  return `${titleKebab}--${nameKebab}`;
}

/**
 * Register all available stories with the native module.
 * This allows the Android test to discover stories automatically.
 */
let storiesRegistered = false;
function registerStoriesWithNative() {
  if (storiesRegistered || !StorybookRegistry) {
    return;
  }

  try {
    const stories = getAllStories();
    if (stories.length > 0) {
      StorybookRegistry.registerStories(stories);
      storiesRegistered = true;
      console.log(`Registered ${stories.length} stories with native module`);
    }
  } catch (e) {
    console.warn('Failed to register stories with native module:', e);
  }
}

/**
 * Renders individual Storybook stories for screenshot testing.
 * Uses Storybook's actual rendering pipeline instead of manual component mapping.
 *
 * Format: "ComponentName/StoryName" (e.g., "MyFeature/Initial")
 */
export default function StoryRenderer({ storyName = 'MyFeature/Initial' }: StoryRendererProps) {
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function renderStory() {
      try {
        const storyId = storyNameToId(storyName);

        // Wait for Storybook to be ready and prepare story mappings
        if (!view._idToPrepared || Object.keys(view._idToPrepared).length === 0) {
          await view.createPreparedStoryMapping();
        }

        // Register all stories with native module for test discovery
        registerStoriesWithNative();

        const preparedStory = view._idToPrepared[storyId];

        if (!preparedStory) {
          // List available stories for debugging
          const availableStories = Object.keys(view._idToPrepared || {}).join(', ');
          setError(`Story "${storyId}" not found. Available: ${availableStories}`);
          setLoading(false);
          return;
        }

        // Render the story using Storybook's prepared story
        const { unboundStoryFn, storyContext } = preparedStory;
        const rendered = unboundStoryFn(storyContext);

        setStoryContent(rendered);
        setLoading(false);
      } catch (e) {
        setError(`Error rendering story: ${e}`);
        setLoading(false);
      }
    }

    renderStory();
  }, [storyName]);

  if (loading) {
    return (
      <View style={styles.container}>
        <Text>Loading story...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.container}>
        <Text style={styles.error}>{error}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {storyContent}
    </View>
  );
}

/**
 * Get all available story IDs from Storybook's registry.
 * Returns array of story IDs like ["myfeature--initial", "myfeature--withclicks", ...]
 */
export function getAllStoryIds(): string[] {
  if (!view._storyIndex?.entries) {
    return [];
  }
  return Object.keys(view._storyIndex.entries);
}

/**
 * Get all stories with their metadata.
 * Useful for generating test cases.
 */
export function getAllStories(): Array<{ id: string; title: string; name: string }> {
  if (!view._storyIndex?.entries) {
    return [];
  }

  return Object.entries(view._storyIndex.entries).map(([id, entry]: [string, any]) => ({
    id,
    title: entry.title,
    name: entry.name,
  }));
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  error: {
    color: 'red',
    textAlign: 'center',
    padding: 20,
  },
});
