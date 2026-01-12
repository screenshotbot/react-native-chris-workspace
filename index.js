/**
 * @format
 */

import { AppRegistry } from 'react-native';
import { name as appName } from './app.json';

// Set to true to use Storybook, false to use your app
const USE_STORYBOOK = false;

let RootComponent;

if (USE_STORYBOOK) {
  RootComponent = require('./.rnstorybook').default;
} else {
  RootComponent = require('./App').default;
}

AppRegistry.registerComponent(appName, () => RootComponent);

// Register a separate component for screenshot tests
// This allows tests to render individual stories without launching the full app
const StoryRenderer = require('./StoryRenderer').default;
AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);
