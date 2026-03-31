/**
 * @format
 */

import { AppRegistry, View, Text } from 'react-native';
import { name as appName } from './app.json';

// Configure and register StoryRenderer for screenshot tests
const { view } = require('./.rnstorybook/storybook.requires');
const { configure, StoryRenderer } = require('rn-storybook-auto-screenshots');
configure(view);
AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);

const SimpleTestComponent = () => <View><Text>Hello</Text></View>;
AppRegistry.registerComponent('SimpleTestComponent', () => SimpleTestComponent);

AppRegistry.registerComponent('MyFeature', () => require('./MyFeature').default);
AppRegistry.registerComponent('SwitchFeature', () => require('./SwitchFeature').default);
AppRegistry.registerComponent('TimerFeature', () => require('./TimerFeature').default);
AppRegistry.registerComponent('CoinFlipFeature', () => require('./CoinFlipFeature').default);
AppRegistry.registerComponent('Button', () => require('./.rnstorybook/stories/Button').Button);
AppRegistry.registerComponent('Header', () => require('./.rnstorybook/stories/Header').Header);

// Modes: 'app' | 'storybook'
const MODE = 'app';

const RootComponent = MODE === 'storybook'
  ? require('./.rnstorybook').default
  : require('./App').default;

AppRegistry.registerComponent(appName, () => RootComponent);
