/**
 * StoryRenderer - Uses the rn-storybook-auto-screenshots package
 *
 * This file configures and re-exports the StoryRenderer from the package.
 */
import { view } from './.rnstorybook/storybook.requires';
import { configure, StoryRenderer, getAllStories, getAllStoryIds } from 'rn-storybook-auto-screenshots';

// Configure the package with the Storybook view
configure(view);

// Re-export for use in the app
export default StoryRenderer;
export { getAllStories, getAllStoryIds };
