/**
 * @format
 */

import { AppRegistry } from 'react-native';
import { name as appName } from './app.json';

// Set this to true to enable Storybook mode
const ENABLE_STORYBOOK = false;

let AppComponent;

if (ENABLE_STORYBOOK) {
  const StorybookUI = require('./.storybook').default;
  AppComponent = StorybookUI;
} else {
  AppComponent = require('./App').default;
}

AppRegistry.registerComponent(appName, () => AppComponent);
