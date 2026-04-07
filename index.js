/**
 * @format
 */

import React, { useEffect, useState } from 'react';
import { AppRegistry, NativeModules, View, Text } from 'react-native';
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


// Imports for story variants
const MyFeature = require('./MyFeature').default;
const SwitchFeature = require('./SwitchFeature').default;
const TimerFeature = require('./TimerFeature').default;
const CoinFlipFeature = require('./CoinFlipFeature').default;
const { Button } = require('./.rnstorybook/stories/Button');
const { Header } = require('./.rnstorybook/stories/Header');

const noop = () => {};

/**
 * Higher-order component that wraps a story component and calls
 * ScreenshotHelper.takeScreenshot from the JS side once rendered.
 * requestAnimationFrame defers by one frame so native layout has settled.
 */
const withAutoScreenshot = (name, InnerComponent) => {
  const Wrapper = () => {
    useEffect(() => {
      requestAnimationFrame(() => {
        NativeModules.ScreenshotHelper.takeScreenshot(name);
      });
    }, []);
    return <InnerComponent />;
  };
  return Wrapper;
};

// IsolatedTest component
const ConstructViewTest = () => <View><Text>Hello</Text></View>;
AppRegistry.registerComponent('ConstructViewTest', () => withAutoScreenshot('ConstructViewTest', ConstructViewTest));

// --- Button story variants ---

const ButtonPrimary = () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button primary label="Button" onPress={noop} /></View>;
AppRegistry.registerComponent('Button_Primary', () => withAutoScreenshot('Button_Primary', ButtonPrimary));

const ButtonSecondary = () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button label="Button" onPress={noop} /></View>;
AppRegistry.registerComponent('Button_Secondary', () => withAutoScreenshot('Button_Secondary', ButtonSecondary));

const ButtonLarge = () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button size="large" label="Button" onPress={noop} /></View>;
AppRegistry.registerComponent('Button_Large', () => withAutoScreenshot('Button_Large', ButtonLarge));

const ButtonSmall = () => <View style={{ flex: 1, alignItems: 'flex-start' }}><Button size="small" label="Button" onPress={noop} /></View>;
AppRegistry.registerComponent('Button_Small', () => withAutoScreenshot('Button_Small', ButtonSmall));

// --- Header story variants ---

const HeaderLoggedIn = () => <Header user={{ name: 'Jane Doe' }} onLogin={noop} onLogout={noop} onCreateAccount={noop} />;
AppRegistry.registerComponent('Header_LoggedIn', () => withAutoScreenshot('Header_LoggedIn', HeaderLoggedIn));

const HeaderLoggedOut = () => <Header onLogin={noop} onLogout={noop} onCreateAccount={noop} />;
AppRegistry.registerComponent('Header_LoggedOut', () => withAutoScreenshot('Header_LoggedOut', HeaderLoggedOut));

// --- MyFeature story variants ---

const MyFeatureInitial = () => { const [c, s] = useState(0); return <MyFeature clickCount={c} setClickCount={s} />; };
AppRegistry.registerComponent('MyFeature_Initial', () => withAutoScreenshot('MyFeature_Initial', MyFeatureInitial));

const MyFeatureWithClicks = () => { const [c, s] = useState(5); return <MyFeature clickCount={c} setClickCount={s} />; };
AppRegistry.registerComponent('MyFeature_WithClicks', () => withAutoScreenshot('MyFeature_WithClicks', MyFeatureWithClicks));

const MyFeatureManyClicks = () => { const [c, s] = useState(42); return <MyFeature clickCount={c} setClickCount={s} />; };
AppRegistry.registerComponent('MyFeature_ManyClicks', () => withAutoScreenshot('MyFeature_ManyClicks', MyFeatureManyClicks));

// --- SwitchFeature story variants ---

const SwitchFeatureOff = () => { const [e, s] = useState(false); return <SwitchFeature isEnabled={e} setIsEnabled={s} />; };
AppRegistry.registerComponent('SwitchFeature_Off', () => withAutoScreenshot('SwitchFeature_Off', SwitchFeatureOff));

const SwitchFeatureOn = () => { const [e, s] = useState(true); return <SwitchFeature isEnabled={e} setIsEnabled={s} />; };
AppRegistry.registerComponent('SwitchFeature_On', () => withAutoScreenshot('SwitchFeature_On', SwitchFeatureOn));

// --- TimerFeature story variants ---

const TimerFeatureInitial = () => { const [s, ss] = useState(0); const [r, sr] = useState(false); return <TimerFeature seconds={s} setSeconds={ss} isRunning={r} setIsRunning={sr} />; };
AppRegistry.registerComponent('TimerFeature_Initial', () => withAutoScreenshot('TimerFeature_Initial', TimerFeatureInitial));

const TimerFeatureRunning = () => <TimerFeature seconds={19} isRunning={true} setSeconds={noop} setIsRunning={noop} />;
AppRegistry.registerComponent('TimerFeature_Running', () => withAutoScreenshot('TimerFeature_Running', TimerFeatureRunning));

const TimerFeaturePaused = () => { const [s, ss] = useState(125); const [r, sr] = useState(false); return <TimerFeature seconds={s} setSeconds={ss} isRunning={r} setIsRunning={sr} />; };
AppRegistry.registerComponent('TimerFeature_Paused', () => withAutoScreenshot('TimerFeature_Paused', TimerFeaturePaused));

const TimerFeatureLongDuration = () => { const [s, ss] = useState(3661); const [r, sr] = useState(false); return <TimerFeature seconds={s} setSeconds={ss} isRunning={r} setIsRunning={sr} />; };
AppRegistry.registerComponent('TimerFeature_LongDuration', () => withAutoScreenshot('TimerFeature_LongDuration', TimerFeatureLongDuration));

// --- CoinFlipFeature story variants ---

const CoinFlipDefault = () => { const [r, sr] = useState(''); const [f, sf] = useState(0); return <CoinFlipFeature result={r} setResult={sr} flips={f} setFlips={sf} />; };
AppRegistry.registerComponent('CoinFlipFeature_Default', () => withAutoScreenshot('CoinFlipFeature_Default', CoinFlipDefault));

const CoinFlipHeads = () => { const [r, sr] = useState('HEADS'); const [f, sf] = useState(5); return <CoinFlipFeature result={r} setResult={sr} flips={f} setFlips={sf} />; };
AppRegistry.registerComponent('CoinFlipFeature_Heads', () => withAutoScreenshot('CoinFlipFeature_Heads', CoinFlipHeads));

const CoinFlipTails = () => { const [r, sr] = useState('TAILS'); const [f, sf] = useState(10); return <CoinFlipFeature result={r} setResult={sr} flips={f} setFlips={sf} />; };
AppRegistry.registerComponent('CoinFlipFeature_Tails', () => withAutoScreenshot('CoinFlipFeature_Tails', CoinFlipTails));

const CoinFlipManyFlips = () => { const [r, sr] = useState('HEADS'); const [f, sf] = useState(99); return <CoinFlipFeature result={r} setResult={sr} flips={f} setFlips={sf} />; };
AppRegistry.registerComponent('CoinFlipFeature_ManyFlips', () => withAutoScreenshot('CoinFlipFeature_ManyFlips', CoinFlipManyFlips));

// Modes: 'app' | 'storybook'
const MODE = 'app';

const RootComponent = MODE === 'storybook'
  ? require('./.rnstorybook').default
  : require('./App').default;

AppRegistry.registerComponent(appName, () => RootComponent);
