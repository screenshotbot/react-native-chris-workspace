import React, { useEffect, useRef, useState } from 'react';
import { View, Text, StyleSheet, NativeModules } from 'react-native';

const { StorybookRegistry } = NativeModules;

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

  if (!storybookView) {
    throw new Error(
      'rn-storybook-auto-screenshots: configure() was not called before registerStoriesWithNative(). ' +
      'Call configure(view) during app initialization so stories can be discovered.'
    );
  }

  const stories = getAllStories();
  if (stories.length > 0) {
    StorybookRegistry.registerStories(stories);
    storiesRegistered = true;
    console.log(`Registered ${stories.length} stories with native module`);
  }
}

/**
 * Renders all Storybook stories for screenshot testing, one at a time.
 *
 * JS drives the entire sequence — there are no incoming events from native.
 *
 * Flow:
 *   1. Mount → register stories → createPreparedStoryMapping() once
 *   2. for (story of allStories):
 *        a. setCurrentStoryId(story.id) → React renders the story
 *        b. useEffect fires after commit → resolves the "render done" Promise
 *        c. await notifyStoryReady(story.id) → native takes screenshot,
 *           then resolves this Promise so JS can proceed
 *   3. allStoriesDone() — signals the test thread to exit and unmount
 */
export function StoryRenderer() {
  const [currentStoryId, setCurrentStoryId] = useState<string | null>(null);
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);

  // Holds the resolve function for the "render done" Promise created in runAllStories().
  // Set before setCurrentStoryId(), called after each commit in the effect below.
  const renderResolverRef = useRef<(() => void) | null>(null);

  // After each story is committed to the view, resolve the pending render Promise
  // so the story loop can proceed to notify native.
  useEffect(() => {
    if (renderResolverRef.current) {
      renderResolverRef.current();
      renderResolverRef.current = null;
    }
  }, [storyContent, error]);

  // Render the story for the current storyId.
  useEffect(() => {
    if (currentStoryId === null) return;

    async function renderStory() {
      try {
        const preparedStory = storybookView._idToPrepared[currentStoryId!];
        if (!preparedStory) {
          const available = Object.keys(storybookView._idToPrepared || {}).join(', ');
          setError(`Story "${currentStoryId}" not found. Available: ${available}`);
          setStoryContent(null);
          return;
        }
        const storyContext = storybookView._preview.getStoryContext(preparedStory);
        const { unboundStoryFn: StoryComponent } = preparedStory;
        setError(null);
        setStoryContent(<StoryComponent {...storyContext} />);
      } catch (e) {
        setError(`Error rendering story: ${e}`);
        setStoryContent(null);
      }
    }

    renderStory();
  }, [currentStoryId]);

  // Main story loop — runs once on mount.
  useEffect(() => {
    async function runAllStories() {
      if (!storybookView) {
        setError('Storybook not configured. Call configure(view) first.');
        return;
      }

      // Write the story list to disk and build the prepared story mapping.
      registerStoriesWithNative();
      await storybookView.createPreparedStoryMapping();

      const stories = getAllStories();
      for (const story of stories) {
        // Ask React to render this story.
        // The Promise resolves only after the commit (in the useEffect above),
        // so we don't notify native until the view is actually painted.
        await new Promise<void>((resolve) => {
          renderResolverRef.current = resolve;
          setCurrentStoryId(story.id);
        });

        // Hand off to native — resolves when the test thread has taken the screenshot.
        await StorybookRegistry.notifyStoryReady(story.id);
      }

      // Tell the test thread there are no more stories.
      StorybookRegistry.allStoriesDone();
    }

    runAllStories();
  }, []);

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

  return Object.entries(storybookView._storyIndex.entries)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([id, entry]: [string, any]) => ({
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
