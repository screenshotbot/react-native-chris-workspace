import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, NativeModules } from 'react-native';
import { storyNameToId } from './utils';

const { StorybookRegistry } = NativeModules;

// Storybook view instance - must be set via configure()
let storybookView: any = null;

/**
 * Configure the library with the Storybook view instance.
 * Call this once during app initialization.
 *
 * @example
 * import { view } from './.rnstorybook/storybook.requires';
 * import { configure } from 'rn-storybook-auto-screenshots';
 * configure(view);
 */
export function configure(view: any) {
  storybookView = view;
}

type StoryRendererProps = {
  storyName?: string;
};

/**
 * Register all available stories with the native module.
 * This allows the Android test to discover stories automatically.
 */
let storiesRegistered = false;
/** @internal Exported for testing only */
export function registerStoriesWithNative() {
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
 * Uses Storybook's actual rendering pipeline.
 *
 * @param storyName - Format: "ComponentName/StoryName" (e.g., "MyFeature/Initial")
 */
export function StoryRenderer({ storyName = 'MyFeature/Initial' }: StoryRendererProps) {
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function renderStory() {
      try {
        if (!storybookView) {
          setError('Storybook not configured. Call configure(view) first.');
          setLoading(false);
          return;
        }

        const storyId = storyNameToId(storyName);

        // Wait for Storybook to be ready and prepare story mappings
        if (!storybookView._idToPrepared || Object.keys(storybookView._idToPrepared).length === 0) {
          await storybookView.createPreparedStoryMapping();
        }

        // Register all stories with native module for test discovery
        registerStoriesWithNative();

        const preparedStory = storybookView._idToPrepared[storyId];

        if (!preparedStory) {
          const availableStories = Object.keys(storybookView._idToPrepared || {}).join(', ');
          setError(`Story "${storyId}" not found. Available: ${availableStories}`);
          setLoading(false);
          return;
        }

        // Get the full story context from Storybook's preview
        const storyContext = storybookView._preview.getStoryContext(preparedStory);

        // Render the story using Storybook's prepared story
        const { unboundStoryFn: StoryComponent } = preparedStory;
        const rendered = <StoryComponent {...storyContext} />;

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
      <View
        style={styles.storyContent}
        onLayout={(e) => {
          const height = e.nativeEvent.layout.height;
          if (StorybookRegistry) {
            StorybookRegistry.setContentHeight(height);
          }
        }}
      >
        {storyContent}
      </View>
    </View>
  );
}

/**
 * Get all available story IDs from Storybook's registry.
 */
export function getAllStoryIds(): string[] {
  if (!storybookView?._storyIndex?.entries) {
    return [];
  }
  return Object.keys(storybookView._storyIndex.entries);
}

/**
 * Get all stories with their metadata.
 */
export function getAllStories(): Array<{ id: string; title: string; name: string }> {
  if (!storybookView?._storyIndex?.entries) {
    return [];
  }

  return Object.entries(storybookView._storyIndex.entries).map(([id, entry]: [string, any]) => ({
    id,
    title: entry.title,
    name: entry.name,
  }));
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'flex-start',
    alignItems: 'stretch',
    backgroundColor: '#FFFFFF',
  },
  storyContent: {
    width: '100%',
  },
  error: {
    color: 'red',
    textAlign: 'center',
    padding: 20,
  },
});
