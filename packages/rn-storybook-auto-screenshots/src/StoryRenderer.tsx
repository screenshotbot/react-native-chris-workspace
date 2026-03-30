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
 * Builds the prepared story mapping without waiting for _preview.ready().
 *
 * createPreparedStoryMapping() waits on storeInitializationPromise, which only
 * resolves when the Storybook UI renders. In our test scenario the UI never
 * renders, so it hangs. We bypass it by going directly to storyStoreValue,
 * which is set synchronously during app startup before our surface mounts.
 * The importFn is async (importPath) => importMap[importPath] — the map is
 * eagerly loaded, so each loadStory call resolves in the next microtask.
 */
async function buildPreparedStories() {
  if (Object.keys(storybookView._idToPrepared).length > 0) {
    return; // already populated (e.g. by Storybook's own updateView())
  }

  const storyStore = storybookView._preview.storyStoreValue;
  if (storyStore) {
    await Promise.all(
      Object.keys(storybookView._storyIndex.entries).map(async (storyId: string) => {
        storybookView._idToPrepared[storyId] = await storyStore.loadStory({ storyId });
      })
    );
  } else {
    // storyStore not yet set — fall back to the standard path.
    await storybookView.createPreparedStoryMapping();
  }
}

/**
 * Renders all Storybook stories for screenshot testing, one at a time.
 *
 * JS drives the entire sequence — no events from native, no blocking sync calls.
 *
 * Flow:
 *   1. Mount → register stories → buildPreparedStories() (sync-ish, no ready() wait)
 *   2. setCurrentStoryId(stories[0].id) → React renders
 *   3. useEffect([storyContent, error]) fires after commit → notifyStoryReady(id)
 *      → native takes screenshot, resolves Promise → advance to next story
 *   4. Repeat until all stories done → allStoriesDone()
 */
export function StoryRenderer() {
  const [currentStoryId, setCurrentStoryId] = useState<string | null>(null);
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);

  // Story list and current position, set once during init.
  const storiesRef = useRef<Array<{ id: string }>>([]);
  const indexRef = useRef(-1); // -1 = not started yet

  // After each story is committed to the view, notify native directly —
  // no intermediate Promise, same pattern as the original notifyStoryReady call.
  // When native resolves (screenshot taken), advance to the next story.
  useEffect(() => {
    if (indexRef.current < 0) return; // init hasn't set the first story yet

    const story = storiesRef.current[indexRef.current];
    if (!story) return;

    StorybookRegistry.notifyStoryReady(story.id).then(() => {
      const next = indexRef.current + 1;
      indexRef.current = next;
      if (next < storiesRef.current.length) {
        setCurrentStoryId(storiesRef.current[next].id);
      } else {
        StorybookRegistry.allStoriesDone();
      }
    });
  }, [storyContent, error]);

  // Render the prepared story synchronously — _idToPrepared is built before
  // the first setCurrentStoryId call, so no async work needed here.
  useEffect(() => {
    if (currentStoryId === null) return;

    try {
      const preparedStory = storybookView._idToPrepared[currentStoryId];
      if (!preparedStory) {
        setError(`Story "${currentStoryId}" not found`);
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
  }, [currentStoryId]);

  // Bootstrap: build prepared stories once, then kick off the story loop.
  useEffect(() => {
    async function init() {
      if (!storybookView) {
        setError('Storybook not configured. Call configure(view) first.');
        return;
      }

      registerStoriesWithNative();
      await buildPreparedStories();

      const stories = getAllStories();
      storiesRef.current = stories;

      if (stories.length === 0) {
        StorybookRegistry.allStoriesDone();
        return;
      }

      indexRef.current = 0;
      setCurrentStoryId(stories[0].id);
    }

    init();
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
