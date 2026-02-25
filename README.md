# rn-storybook-auto-screenshots

Automatically generate screenshot tests for every Storybook story in your React Native app.

## How it works

1. On first test run, the library launches your app and lets React Native register all Storybook stories with a native module (`StorybookRegistry`)
2. The native module writes a `storybook_stories.json` manifest to device storage
3. The test runner reads the manifest and launches a `StoryRendererActivity` for each story, capturing a screenshot with [screenshot-tests-for-android](https://github.com/screenshotbot/screenshot-tests-for-android)

No manual list of stories needed — add a story to Storybook and it gets tested automatically.

## Installation

```sh
npm install rn-storybook-auto-screenshots
```

## Setup

### 1. JS side — configure and register

In your app's entry point (e.g. `index.js`):

```js
import { AppRegistry } from 'react-native';
import { configure, StoryRenderer } from 'rn-storybook-auto-screenshots';
import { view } from './.rnstorybook/storybook.requires';

configure(view);

AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);
AppRegistry.registerComponent('YourApp', () => YourApp);
```

### 2. Android — create the activity

In your app's `android/app/src/main/java/…` directory:

```kotlin
class StoryRendererActivity : BaseStoryRendererActivity()
```

Register it in `AndroidManifest.xml`:

```xml
<activity android:name=".StoryRendererActivity" />
```

Add the native module to your React Native package list:

```kotlin
// In your MainApplication
override fun getPackages() = listOf(
    RNStorybookAutoScreenshotsPackage(),
    // ... your other packages
)
```

### 3. Android — create the test

In `android/app/src/androidTest/java/…`:

```kotlin
@RunWith(AndroidJUnit4::class)
class StoryScreenshotTest : BaseStoryScreenshotTest() {
    override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java
}
```

### 4. Run the tests

```sh
./gradlew screenshotTests
```

## Customization

Override methods in `BaseStoryScreenshotTest` to customize behavior:

```kotlin
class StoryScreenshotTest : BaseStoryScreenshotTest() {
    override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java

    // Skip stories you don't want to screenshot
    override fun shouldScreenshotStory(storyInfo: StoryInfo): Boolean {
        return storyInfo.title != "Internal"
    }

    // Adjust timeout if stories are slow to load (default: 5000ms)
    override fun getLoadTimeoutMs() = 8000L
}
```

## JS API

```ts
import { configure, StoryRenderer, getAllStories, getAllStoryIds, storyNameToId } from 'rn-storybook-auto-screenshots';

configure(view);           // Must be called once with your Storybook view instance
getAllStories();            // Returns [{ id, title, name }]
getAllStoryIds();           // Returns string[]
storyNameToId('Foo/Bar');  // Converts "ComponentName/StoryName" to a Storybook ID
```

## License

Apache-2.0
