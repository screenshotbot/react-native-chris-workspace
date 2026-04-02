/**
 * @format
 */

import { AppRegistry, View, Text } from 'react-native';
import { name as appName } from './app.json';

// Configure and register StoryRenderer for screenshot tests
const { view } = require('./.rnstorybook/storybook.requires');

// TODO: bring this back. It was failing on Arnold's Android Studio builds, so while we were working on IsolatedTest,
// we disabled it to keep the builds fast.
// const { configure, StoryRenderer } = require('rn-storybook-auto-screenshots');
// configure(view);
// AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);

const SimpleTestComponent = () => <View><Text>Hello</Text></View>;
AppRegistry.registerComponent('SimpleTestComponent', () => SimpleTestComponent);

// Modes: 'app' | 'storybook'
const MODE = 'app';

const RootComponent = MODE === 'storybook'
  ? require('./.rnstorybook').default
  : require('./App').default;

AppRegistry.registerComponent(appName, () => RootComponent);
