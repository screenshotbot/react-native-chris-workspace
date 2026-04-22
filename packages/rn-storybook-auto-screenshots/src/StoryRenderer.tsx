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
 * Builds the prepared story mapping without waiting for _preview.ready().
 *
 * createPreparedStoryMapping() waits on storeInitializationPromise, which only
 * resolves when the Storybook UI renders. In our test scenario the UI never
 * renders, so it hangs. We bypass it by going directly to storyStoreValue,
 * which is set synchronously during app startup before our surface mounts.
 */
async function buildPreparedStories() {
  if (Object.keys(storybookView._idToPrepared).length > 0) {
    return; // already populated
  }

  const storyStore = storybookView._preview.storyStoreValue;
  if (storyStore) {
    await Promise.all(
      Object.keys(storybookView._storyIndex.entries).map(async (storyId: string) => {
        storybookView._idToPrepared[storyId] = await storyStore.loadStory({ storyId });
      })
    );
  } else {
    await storybookView.createPreparedStoryMapping();
  }
}

/**
 * Renders Storybook stories for screenshot testing.
 *
 * Native controls the story order by pushing IDs into a queue. JS pulls each
 * ID by awaiting awaitNextStory() (Promise resolved on a background thread),
 * renders the story, notifies native via notifyStoryReady(), then awaits the
 * next ID. This inverts the event-push model: JS pulls rather than native pushing.
 *
 * Flow:
 *   1. Mount → register stories → await awaitNextStory() (waits for queue)
 *   2. Test thread pushes story ID → Promise resolves → JS renders
 *   3. After commit: notifyStoryReady() releases native latch
 *   4. await awaitNextStory() again — native takes screenshot, then pushes next ID
 *   5. When native pushes null, the loop ends
 */
export function StoryRenderer() {
  const [currentStoryId, setCurrentStoryId] = useState<string | null>(null);
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);

  // Bootstrap: register stories then wait for the first story ID from native.
  useEffect(() => {
    async function init() {
      if (!storybookView) {
        setError('Storybook not configured. Call configure(view) first.');
        return;
      }

      registerStoriesWithNative();

      // awaitNextStory() resolves on a background thread once the test runner
      // pushes the first story ID via pushStory().
      const firstId: string | null = await StorybookRegistry.awaitNextStory();
      if (firstId !== null) {
        setCurrentStoryId(firstId);
      }
    }

    init();
  }, []);

  // Render the story for the current storyId.
  // buildPreparedStories() is called lazily here to handle the first real story
  // (bootstrap ID "__bootstrap__" won't have a prepared entry — that's expected).
  useEffect(() => {
    if (currentStoryId === null) return;

    async function renderStory() {
      try {
        await buildPreparedStories();

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

  // After each story commit: signal native, then await the next story ID.
  // notifyStoryReady() releases the latch the test thread is waiting on.
  // awaitNextStory() blocks (on a background thread) until native pushes the
  // next ID — giving native time to take the screenshot first.
  useEffect(() => {
    if (currentStoryId === null) return;

    async function advance() {
      StorybookRegistry.notifyStoryReady();
      const nextId: string | null = await StorybookRegistry.awaitNextStory();
      if (nextId !== null) {
        setCurrentStoryId(nextId);
      }
      // else: null means all stories are done — stop here.
    }

    advance();
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
