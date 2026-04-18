/**
 * @format
 */

import { AppRegistry, View, Text } from 'react-native';
import { name as appName } from './app.json';


const SimpleTestComponent = () => <View><Text>Hello</Text></View>;
AppRegistry.registerComponent('SimpleTestComponent', () => SimpleTestComponent);

// Modes: 'app' | 'storybook'
const MODE = 'app';

const RootComponent = MODE === 'storybook'
  ? require('./.rnstorybook').default
  : require('./App').default;

AppRegistry.registerComponent(appName, () => RootComponent);
