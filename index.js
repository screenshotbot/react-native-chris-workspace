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
