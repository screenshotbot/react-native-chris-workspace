/**
 * @format
 */

import { AppRegistry } from 'react-native';
import { name as appName } from './app.json';

// Modes: 'app' | 'storybook' | 'story-renderer-test'
const MODE = 'app';

let RootComponent;

if (MODE === 'storybook') {
  RootComponent = require('./.rnstorybook').default;
} else if (MODE === 'story-renderer-test') {
  // Test the StoryRenderer component
  const { view } = require('./.rnstorybook/storybook.requires');
  const { configure, StoryRenderer } = require('rn-storybook-auto-screenshots');
  configure(view);

  // Render a specific story to test
  RootComponent = () => StoryRenderer({ storyName: 'MyFeature/Initial' });
} else {
  RootComponent = require('./App').default;
}

// Register main app component
AppRegistry.registerComponent(appName, () => RootComponent);

// Register StoryRenderer component for screenshot tests
// This is used by StoryRendererActivity to render individual stories
const { view } = require('./.rnstorybook/storybook.requires');
const { configure, StoryRenderer } = require('rn-storybook-auto-screenshots');
configure(view);
AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);
