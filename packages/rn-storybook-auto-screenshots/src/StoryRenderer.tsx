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
  console.log('[StoryRenderer] configure() called with view');

  // Try to register stories after a delay, as a fallback
  // This helps ensure stories are registered even if the component rendering fails
  setTimeout(async () => {
    console.log('[StoryRenderer] Delayed registration attempt...');
    try {
      // Try to prepare stories if not already done
      if (storybookView && (!storybookView._idToPrepared || Object.keys(storybookView._idToPrepared).length === 0)) {
        if (typeof storybookView.createPreparedStoryMapping === 'function') {
          console.log('[StoryRenderer] Calling createPreparedStoryMapping from configure...');
          await storybookView.createPreparedStoryMapping();
        }
      }
      registerStoriesWithNative();
    } catch (e) {
      console.error('[StoryRenderer] Delayed registration failed:', e);
    }
  }, 5000);
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
  if (storiesRegistered) {
    console.log('[StoryRenderer] Stories already registered, skipping');
    return;
  }

  if (!StorybookRegistry) {
    console.warn('[StoryRenderer] StorybookRegistry native module not available');
    return;
  }

  try {
    // Log available data sources for debugging
    const hasStoryIndex = !!storybookView?._storyIndex?.entries;
    const hasIdToPrepared = !!storybookView?._idToPrepared;
    console.log(`[StoryRenderer] Data sources: _storyIndex=${hasStoryIndex}, _idToPrepared=${hasIdToPrepared}`);

    const stories = getAllStories();
    console.log(`[StoryRenderer] Found ${stories.length} stories`);

    if (stories.length > 0) {
      StorybookRegistry.registerStories(stories);
      storiesRegistered = true;
      console.log(`[StoryRenderer] Registered ${stories.length} stories with native module`);
    } else {
      console.warn('[StoryRenderer] No stories found to register');
    }
  } catch (e) {
    console.error('[StoryRenderer] Failed to register stories with native module:', e);
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
          console.error('[StoryRenderer] Storybook not configured. Call configure(view) first.');
          setError('Storybook not configured. Call configure(view) first.');
          setLoading(false);
          return;
        }

        const storyId = storyNameToId(storyName);
        console.log(`[StoryRenderer] Rendering story: ${storyName} (id: ${storyId})`);

        // Wait for Storybook to be ready and prepare story mappings
        if (!storybookView._idToPrepared || Object.keys(storybookView._idToPrepared).length === 0) {
          console.log('[StoryRenderer] Calling createPreparedStoryMapping...');
          try {
            await storybookView.createPreparedStoryMapping();
            console.log('[StoryRenderer] createPreparedStoryMapping completed');
          } catch (prepError) {
            console.error('[StoryRenderer] createPreparedStoryMapping failed:', prepError);
            // Continue anyway - try to register what we have
          }
        }

        // Register all stories with native module for test discovery
        // Do this before checking for the specific story so manifest gets created even if story lookup fails
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
      {storyContent}
    </View>
  );
}

/**
 * Get all available story IDs from Storybook's registry.
 */
export function getAllStoryIds(): string[] {
  // Try _storyIndex first, then fall back to _idToPrepared
  if (storybookView?._storyIndex?.entries) {
    return Object.keys(storybookView._storyIndex.entries);
  }
  if (storybookView?._idToPrepared) {
    return Object.keys(storybookView._idToPrepared);
  }
  return [];
}

/**
 * Parse story ID to extract title and name.
 * Story IDs are in format "title--name" (lowercase, hyphenated).
 */
function parseStoryId(id: string): { title: string; name: string } {
  const parts = id.split('--');
  if (parts.length >= 2) {
    // Convert kebab-case back to Title Case
    const title = parts[0].split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join('');
    const name = parts.slice(1).join('--').split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join('');
    return { title, name };
  }
  return { title: id, name: 'Default' };
}

/**
 * Get all stories with their metadata.
 * Tries _storyIndex.entries first, falls back to _idToPrepared with ID parsing.
 */
export function getAllStories(): Array<{ id: string; title: string; name: string }> {
  // Prefer _storyIndex.entries as it has proper metadata
  if (storybookView?._storyIndex?.entries) {
    return Object.entries(storybookView._storyIndex.entries).map(([id, entry]: [string, any]) => ({
      id,
      title: entry.title,
      name: entry.name,
    }));
  }

  // Fall back to _idToPrepared and try to get metadata from prepared story or parse ID
  if (storybookView?._idToPrepared) {
    return Object.entries(storybookView._idToPrepared).map(([id, preparedStory]: [string, any]) => {
      // Try to get title/name from prepared story properties
      if (preparedStory.title && preparedStory.name) {
        return { id, title: preparedStory.title, name: preparedStory.name };
      }
      // Fall back to parsing the ID
      const parsed = parseStoryId(id);
      return { id, title: parsed.title, name: parsed.name };
    });
  }

  return [];
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
