import React, { useEffect, useState } from 'react';
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
 * Renders Storybook stories for screenshot testing.
 *
 * Instead of receiving a story name via prop or event, this component drives
 * itself by calling the synchronous native method awaitNextStory(), which
 * blocks the JS thread until the test runner enqueues the next story ID.
 *
 * Flow:
 *   1. Mount → register stories → call awaitNextStory() (blocks JS thread)
 *   2. Test thread pushes story ID into the queue → JS unblocks
 *   3. React re-renders the story
 *   4. After commit: notifyStoryReady() releases native latch, then
 *      awaitNextStory() blocks again for the next story
 *   5. When native pushes null, the loop ends
 */
export function StoryRenderer() {
  const [currentStoryId, setCurrentStoryId] = useState<string | null>(null);
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);

  // Bootstrap: register stories then block waiting for the first story ID.
  useEffect(() => {
    if (!storybookView) {
      setError('Storybook not configured. Call configure(view) first.');
      return;
    }

    registerStoriesWithNative();

    // awaitNextStory() is a blocking synchronous JSI call — it returns only
    // when the test thread pushes a story ID (or null to signal done).
    const firstId: string | null = StorybookRegistry.awaitNextStory();
    if (firstId !== null) {
      setCurrentStoryId(firstId);
    }
  }, []);

  // Render the current story whenever currentStoryId changes.
  useEffect(() => {
    if (currentStoryId === null) return;

    async function renderStory() {
      try {
        // Lazily populate _idToPrepared — createPreparedStoryMapping() is async.
        if (!storybookView._idToPrepared || Object.keys(storybookView._idToPrepared).length === 0) {
          await storybookView.createPreparedStoryMapping();
        }

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

  // After each story commit: signal native, then block waiting for the next story.
  // This effect fires once storyContent or error has settled for the current story.
  useEffect(() => {
    if (currentStoryId === null) return;

    // Signal native that the current story is fully rendered.
    StorybookRegistry.notifyStoryReady();

    // Block the JS thread until the test runner pushes the next story ID.
    // While blocked, native takes the screenshot and advances its loop.
    const nextId: string | null = StorybookRegistry.awaitNextStory();
    if (nextId !== null) {
      setCurrentStoryId(nextId);
    }
    // else: null means all stories are done — we stop here.
  }, [storyContent, error]);

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
