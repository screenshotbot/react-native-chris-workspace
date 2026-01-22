/**
 * @format
 */

import { AppRegistry } from 'react-native';
import { name as appName } from './app.json';

// Modes: 'app' | 'storybook' | 'story-renderer-test'
const MODE = 'story-renderer-test';

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

AppRegistry.registerComponent(appName, () => RootComponent);
