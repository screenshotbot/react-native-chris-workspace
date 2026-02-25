# rn-storybook-auto-screenshots

Auto-detect Storybook stories and generate screenshot tests for React Native.

Uses [screenshot-tests-for-android](https://github.com/screenshotbot/screenshot-tests-for-android) under the hood to capture screenshots of every Storybook story automatically.

## Installation

```bash
npm install rn-storybook-auto-screenshots
```

## Setup

### 1. JS — configure and register

In your app's entry point (e.g. `index.js`):

```typescript
import { AppRegistry } from 'react-native';
import { configure, StoryRenderer } from 'rn-storybook-auto-screenshots';
import { view } from './.rnstorybook/storybook.requires';

configure(view);

AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);
```

### 2. Android — add the native module

In your `MainApplication`:

```kotlin
override fun getPackages() = listOf(
    RNStorybookAutoScreenshotsPackage(),
    // ... your other packages
)
```

### 3. Android — create the story renderer activity

Create `StoryRendererActivity.kt` in your main source set:

```kotlin
class StoryRendererActivity : BaseStoryRendererActivity()
```

Register it in `AndroidManifest.xml`:

```xml
<activity
    android:name=".StoryRendererActivity"
    android:exported="false" />
```

### 4. Android — create the test runner

Create `ScreenshotTestRunner.kt` in your `androidTest` directory:

```kotlin
class ScreenshotTestRunner : BaseScreenshotTestRunner()
```

Reference it in your `build.gradle`:

```gradle
android {
    defaultConfig {
        testInstrumentationRunner "com.yourapp.ScreenshotTestRunner"
    }
}
```

### 5. Android — create the screenshot test

Create `StoryScreenshotTest.kt` in your `androidTest` directory:

```kotlin
@RunWith(AndroidJUnit4::class)
class StoryScreenshotTest : BaseStoryScreenshotTest() {
    override fun getStoryRendererActivityClass() = StoryRendererActivity::class.java
}
```

### 6. Run the tests

```bash
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

    // Adjust per-story load timeout (default: 5000ms)
    override fun getLoadTimeoutMs() = 8000L

    // Adjust bootstrap timeout for initial story discovery (default: 10000ms)
    override fun getBootstrapTimeoutMs() = 15000L
}
```

## JS API

| Export | Description |
|--------|-------------|
| `configure(view)` | Configure with your Storybook view instance. Call once at startup. |
| `<StoryRenderer storyName="Title/Name" />` | Renders a single story. Used by the Android test infrastructure. |
| `getAllStories()` | Returns `[{ id, title, name }]` for all registered stories. |
| `getAllStoryIds()` | Returns `string[]` of all story IDs. |
| `storyNameToId("Title/Name")` | Converts `"MyFeature/Initial"` → `"myfeature--initial"`. |

## How it works

On the first test run, the library bootstraps itself:

1. Launches `StoryRendererActivity` with a known story
2. React Native initializes and `StorybookRegistry` writes all story metadata to a JSON file on device storage
3. The test reads the manifest and loops through every story, launching the activity for each and capturing a screenshot

No manual list of stories needed — add a story to Storybook and it gets tested automatically.

## License

Apache-2.0
