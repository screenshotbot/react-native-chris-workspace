/**
 * @format
 */

import { AppRegistry } from 'react-native';
import { name as appName } from './app.json';

// Configure and register StoryRenderer for screenshot tests
const { view } = require('./.rnstorybook/storybook.requires');
const { configure, StoryRenderer } = require('rn-storybook-auto-screenshots');
configure(view);
AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);

const { SimpleComponent } = require('rn-storybook-auto-screenshots');
AppRegistry.registerComponent('SimpleComponent', () => SimpleComponent);

// Modes: 'app' | 'storybook'
const MODE = 'app';

const RootComponent = MODE === 'storybook'
  ? require('./.rnstorybook').default
  : require('./App').default;

AppRegistry.registerComponent(appName, () => RootComponent);
