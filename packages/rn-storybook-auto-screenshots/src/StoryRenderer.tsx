import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, NativeModules } from 'react-native';

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
 * Converts a story name like "MyFeature/Initial" to Storybook's ID format "myfeature--initial"
 */
function storyNameToId(storyName: string): string {
  const [title, name] = storyName.split('/');
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
 * Uses Storybook's actual rendering pipeline.
 *
 * @param storyName - Format: "ComponentName/StoryName" (e.g., "MyFeature/Initial")
 */
export function StoryRenderer({ storyName = 'MyFeature/Initial' }: StoryRendererProps) {
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [debugInfo, setDebugInfo] = useState<string>('initializing');

  useEffect(() => {
    async function renderStory() {
      try {
        console.log('[StoryRenderer] Starting render for:', storyName);
        setDebugInfo(`loading: ${storyName}`);

        if (!storybookView) {
          console.log('[StoryRenderer] ERROR: storybookView not configured');
          setError('Storybook not configured. Call configure(view) first.');
          setLoading(false);
          return;
        }

        const storyId = storyNameToId(storyName);
        console.log('[StoryRenderer] Story ID:', storyId);
        setDebugInfo(`storyId: ${storyId}`);

        // Wait for Storybook to be ready and prepare story mappings
        if (!storybookView._idToPrepared || Object.keys(storybookView._idToPrepared).length === 0) {
          console.log('[StoryRenderer] Creating prepared story mapping...');
          setDebugInfo('creating story mapping...');
          await storybookView.createPreparedStoryMapping();
          console.log('[StoryRenderer] Story mapping created');
        }

        // Register all stories with native module for test discovery
        registerStoriesWithNative();

        const preparedStory = storybookView._idToPrepared[storyId];
        console.log('[StoryRenderer] Found prepared story:', !!preparedStory);

        if (!preparedStory) {
          const availableStories = Object.keys(storybookView._idToPrepared || {}).join(', ');
          console.log('[StoryRenderer] Available stories:', availableStories);
          setError(`Story "${storyId}" not found. Available: ${availableStories}`);
          setLoading(false);
          return;
        }

        // Get the full story context from Storybook's preview
        const storyContext = storybookView._preview.getStoryContext(preparedStory);
        console.log('[StoryRenderer] Got story context');

        // Render the story using Storybook's prepared story
        const { unboundStoryFn: StoryComponent } = preparedStory;
        console.log('[StoryRenderer] Got StoryComponent:', typeof StoryComponent);

        const rendered = <StoryComponent {...storyContext} />;

        setStoryContent(rendered);
        setDebugInfo(`rendered: ${storyName}`);
        setLoading(false);
        console.log('[StoryRenderer] Story rendered successfully');
      } catch (e) {
        console.log('[StoryRenderer] ERROR:', e);
        setError(`Error rendering story: ${e}`);
        setLoading(false);
      }
    }

    renderStory();
  }, [storyName]);

  if (loading) {
    return (
      <View style={[styles.container, styles.debugLoading]}>
        <Text style={styles.debugText}>Loading: {storyName}</Text>
        <Text style={styles.debugText}>Debug: {debugInfo}</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={[styles.container, styles.debugError]}>
        <Text style={styles.error}>{error}</Text>
      </View>
    );
  }

  return (
    <View style={styles.storyContainer}>
      {storyContent}
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
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  storyContainer: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  debugLoading: {
    backgroundColor: '#FFFFCC', // Yellow tint for loading state
  },
  debugError: {
    backgroundColor: '#FFCCCC', // Red tint for error state
  },
  debugText: {
    fontSize: 16,
    color: '#666666',
    textAlign: 'center',
    padding: 10,
  },
  error: {
    color: 'red',
    textAlign: 'center',
    padding: 20,
    fontSize: 14,
  },
});
