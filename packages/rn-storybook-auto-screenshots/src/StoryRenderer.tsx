import React, { useEffect, useState, useRef } from 'react';
import { View, Text, StyleSheet, NativeModules } from 'react-native';

const { StorybookRegistry } = NativeModules;

let storybookView: any = null;

/**
 * Promise that resolves once all stories have been loaded into _idToPrepared.
 * Kicked off synchronously in configure() so it is resolved (or nearly so)
 * before StoryRenderer ever mounts.
 */
let eagerPrepPromise: Promise<void> | null = null;

/**
 * Configure the library with the Storybook view instance.
 * Call this once during app initialization.
 *
 * Immediately starts loading all stories into _idToPrepared using
 * storyStoreValue — bypassing _preview.ready() which only resolves
 * when the Storybook UI renders (never in a test run).
 *
 * @example
 * import { view } from './.rnstorybook/storybook.requires';
 * import { configure } from 'rn-storybook-auto-screenshots';
 * configure(view);
 */
export function configure(view: any) {
  storybookView = view;

  // importFn is `async path => importMap[path]` — the map is already loaded
  // synchronously at app startup, so each loadStory() resolves in one microtask.
  // We fan-out all calls immediately so they run in parallel.
  const storyStore = view._preview?.storyStoreValue;
  if (storyStore && view._storyIndex?.entries) {
    const storyIds = Object.keys(view._storyIndex.entries);
    eagerPrepPromise = Promise.all(
      storyIds.map((id: string) => storyStore.loadStory({ storyId: id }))
    ).then((prepared: any[]) => {
      storyIds.forEach((id: string, i: number) => {
        view._idToPrepared[id] = prepared[i];
      });
    });
  }
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
 * Renders all Storybook stories sequentially for screenshot testing.
 *
 * Story preparation is done eagerly at configure() time, so the render
 * effect for each story is fully synchronous — no async/await in the
 * hot path between "set story" and "notify native".
 *
 * Protocol with native:
 *   JS calls  StorybookRegistry.notifyStoryReady(id)  → Promise
 *   Native screenshots, then resolves the Promise
 *   JS advances to the next story
 *   When all stories are done, JS calls  StorybookRegistry.allStoriesDone()
 */
export function StoryRenderer() {
  const [currentStoryId, setCurrentStoryId] = useState<string | null>(null);
  const [storyContent, setStoryContent] = useState<React.ReactNode>(null);
  const [error, setError] = useState<string | null>(null);

  // Stable refs so effect callbacks always see the latest values without
  // needing them in their dependency arrays.
  const storiesRef = useRef<Array<{ id: string }>>([]);
  const indexRef = useRef(-1);

  // After React commits a story render (or error), notify native so the test
  // thread can screenshot, then advance to the next story once it resolves.
  useEffect(() => {
    if (indexRef.current < 0) return;
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

  // Render the current story synchronously.
  // _idToPrepared is fully populated by eagerPrepPromise before we get here,
  // so no await is needed — this effect is pure synchronous state work.
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

  // Bootstrap: register stories with native, wait for eager prep (safety net
  // in case configure() was called very close to mount), then start the loop.
  useEffect(() => {
    async function init() {
      if (!storybookView) {
        setError('Storybook not configured. Call configure(view) first.');
        return;
      }

      // Write the manifest synchronously — _storyIndex is available immediately.
      registerStoriesWithNative();

      // Await eager prep. In normal usage this is already resolved by now.
      if (eagerPrepPromise) {
        await eagerPrepPromise;
      }

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

  if (indexRef.current < 0) {
    return (
      <View style={styles.container}>
        <Text>Loading stories...</Text>
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
