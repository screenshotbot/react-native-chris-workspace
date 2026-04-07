/**
 * @format
 */

import React, { useState } from 'react';
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

const MyFeature = require('./MyFeature').default;
const SwitchFeature = require('./SwitchFeature').default;
const TimerFeature = require('./TimerFeature').default;
const CoinFlipFeature = require('./CoinFlipFeature').default;
const { Button } = require('./.rnstorybook/stories/Button');
const { Header } = require('./.rnstorybook/stories/Header');

const noop = () => {};

// Base components
AppRegistry.registerComponent('MyFeature', () => MyFeature);
AppRegistry.registerComponent('SwitchFeature', () => SwitchFeature);
AppRegistry.registerComponent('TimerFeature', () => TimerFeature);
AppRegistry.registerComponent('CoinFlipFeature', () => CoinFlipFeature);
AppRegistry.registerComponent('Button', () => Button);
AppRegistry.registerComponent('Header', () => Header);

// Button story variants
AppRegistry.registerComponent('Button_Primary', () => () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button primary label="Button" onPress={noop} /></View>);
AppRegistry.registerComponent('Button_Secondary', () => () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button label="Button" onPress={noop} /></View>);
AppRegistry.registerComponent('Button_Large', () => () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button size="large" label="Button" onPress={noop} /></View>);
AppRegistry.registerComponent('Button_Small', () => () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button size="small" label="Button" onPress={noop} /></View>);

// Header story variants
AppRegistry.registerComponent('Header_LoggedIn', () => () => <Header user={{ name: 'Jane Doe' }} onLogin={noop} onLogout={noop} onCreateAccount={noop} />);
AppRegistry.registerComponent('Header_LoggedOut', () => () => <Header onLogin={noop} onLogout={noop} onCreateAccount={noop} />);

// MyFeature story variants
AppRegistry.registerComponent('MyFeature_Initial', () => () => { const [c, s] = useState(0); return <MyFeature clickCount={c} setClickCount={s} />; });
AppRegistry.registerComponent('MyFeature_WithClicks', () => () => { const [c, s] = useState(5); return <MyFeature clickCount={c} setClickCount={s} />; });
AppRegistry.registerComponent('MyFeature_ManyClicks', () => () => { const [c, s] = useState(42); return <MyFeature clickCount={c} setClickCount={s} />; });

// SwitchFeature story variants
AppRegistry.registerComponent('SwitchFeature_Off', () => () => { const [e, s] = useState(false); return <SwitchFeature isEnabled={e} setIsEnabled={s} />; });
AppRegistry.registerComponent('SwitchFeature_On', () => () => { const [e, s] = useState(true); return <SwitchFeature isEnabled={e} setIsEnabled={s} />; });

// TimerFeature story variants
AppRegistry.registerComponent('TimerFeature_Initial', () => () => { const [sec, setSec] = useState(0); const [run, setRun] = useState(false); return <TimerFeature seconds={sec} setSeconds={setSec} isRunning={run} setIsRunning={setRun} />; });
AppRegistry.registerComponent('TimerFeature_Running', () => () => <TimerFeature seconds={19} isRunning={true} setSeconds={noop} setIsRunning={noop} />);
AppRegistry.registerComponent('TimerFeature_Paused', () => () => { const [sec, setSec] = useState(125); const [run, setRun] = useState(false); return <TimerFeature seconds={sec} setSeconds={setSec} isRunning={run} setIsRunning={setRun} />; });
AppRegistry.registerComponent('TimerFeature_LongDuration', () => () => { const [sec, setSec] = useState(3661); const [run, setRun] = useState(false); return <TimerFeature seconds={sec} setSeconds={setSec} isRunning={run} setIsRunning={setRun} />; });

// CoinFlipFeature story variants
AppRegistry.registerComponent('CoinFlipFeature_Default', () => () => { const [r, setR] = useState(''); const [f, setF] = useState(0); return <CoinFlipFeature result={r} setResult={setR} flips={f} setFlips={setF} />; });
AppRegistry.registerComponent('CoinFlipFeature_Heads', () => () => { const [r, setR] = useState('HEADS'); const [f, setF] = useState(5); return <CoinFlipFeature result={r} setResult={setR} flips={f} setFlips={setF} />; });
AppRegistry.registerComponent('CoinFlipFeature_Tails', () => () => { const [r, setR] = useState('TAILS'); const [f, setF] = useState(10); return <CoinFlipFeature result={r} setResult={setR} flips={f} setFlips={setF} />; });
AppRegistry.registerComponent('CoinFlipFeature_ManyFlips', () => () => { const [r, setR] = useState('HEADS'); const [f, setF] = useState(99); return <CoinFlipFeature result={r} setResult={setR} flips={f} setFlips={setF} />; });

// Modes: 'app' | 'storybook'
const MODE = 'app';

const RootComponent = MODE === 'storybook'
  ? require('./.rnstorybook').default
  : require('./App').default;

AppRegistry.registerComponent(appName, () => RootComponent);
