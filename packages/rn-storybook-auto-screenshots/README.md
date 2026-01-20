# rn-storybook-auto-screenshots

Auto-detect Storybook stories and generate screenshot tests for React Native.

## Installation

```bash
npm install rn-storybook-auto-screenshots
# or
yarn add rn-storybook-auto-screenshots
```

## Setup

### 1. Configure Storybook integration

```typescript
// StoryRenderer.tsx in your app root
import { view } from './.rnstorybook/storybook.requires';
import { configure, StoryRenderer, getAllStories, getAllStoryIds } from 'rn-storybook-auto-screenshots';

configure(view);

export default StoryRenderer;
export { getAllStories, getAllStoryIds };
```

### 2. Register the StoryRenderer component

```typescript
// index.js or App.tsx
import { AppRegistry } from 'react-native';
import StoryRenderer from './StoryRenderer';

AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);
```

### 3. Create StoryRendererActivity (Android)

```kotlin
// android/app/src/main/java/com/yourapp/StoryRendererActivity.kt
package com.yourapp

import com.rnstorybookautoscreenshots.BaseStoryRendererActivity

class StoryRendererActivity : BaseStoryRendererActivity()
```

Register it in your AndroidManifest.xml:

```xml
<activity
    android:name=".StoryRendererActivity"
    android:exported="false" />
```

### 4. Add test dependencies

Add these to your app's `android/app/build.gradle`:

```gradle
dependencies {
    // Screenshot testing dependencies
    androidTestImplementation("io.screenshotbot.screenshot-tests-for-android:core:1.0.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:core:1.5.0")
}
```

### 5. Create test runner

```kotlin
// android/app/src/androidTest/java/com/yourapp/ScreenshotTestRunner.kt
package com.yourapp

import com.rnstorybookautoscreenshots.BaseScreenshotTestRunner

class ScreenshotTestRunner : BaseScreenshotTestRunner()
```

Configure it in `android/app/build.gradle`:

```gradle
android {
    defaultConfig {
        testInstrumentationRunner "com.yourapp.ScreenshotTestRunner"
    }
}
```

### 6. Create screenshot tests

Create a bootstrap test that generates the story manifest:

```kotlin
// android/app/src/androidTest/java/com/yourapp/StoryManifestBootstrapTest.kt
package com.yourapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rnstorybookautoscreenshots.BaseStoryManifestTest
import com.rnstorybookautoscreenshots.BaseStoryRendererActivity
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryManifestBootstrapTest : BaseStoryManifestTest() {
    override fun getStoryRendererActivityClass(): Class<out BaseStoryRendererActivity> =
        StoryRendererActivity::class.java
}
```

Create the screenshot test that auto-discovers all stories:

```kotlin
// android/app/src/androidTest/java/com/yourapp/StoryScreenshotTest.kt
package com.yourapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rnstorybookautoscreenshots.BaseStoryRendererActivity
import com.rnstorybookautoscreenshots.BaseStoryScreenshotTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryScreenshotTest : BaseStoryScreenshotTest() {
    override fun getStoryRendererActivityClass(): Class<out BaseStoryRendererActivity> =
        StoryRendererActivity::class.java
}
```

## Running tests

```bash
# Run the bootstrap test first to generate the story manifest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yourapp.StoryManifestBootstrapTest

# Then run the screenshot tests
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yourapp.StoryScreenshotTest
```

## API

### TypeScript/JavaScript

#### `configure(view)`

Configure the library with your Storybook view instance. Call this once during app initialization.

#### `StoryRenderer`

React component that renders a story by name. Props:
- `storyName`: Story name in format "ComponentName/StoryName" (e.g., "Button/Primary")

#### `getAllStories()`

Returns an array of all stories with their metadata:
```typescript
Array<{ id: string; title: string; name: string }>
```

#### `getAllStoryIds()`

Returns an array of all story IDs.

### Android Base Classes

#### `BaseStoryRendererActivity`

Base activity for rendering stories. Extend this in your app.

#### `BaseScreenshotTestRunner`

Base test runner that configures screenshot testing. Extend this in your app.

#### `BaseStoryManifestTest`

Base test that generates the story manifest. Override:
- `getStoryRendererActivityClass()` - Return your StoryRendererActivity class
- `getInitialStoryName()` - (Optional) Initial story for bootstrapping, default "MyFeature/Initial"
- `getLoadTimeoutMs()` - (Optional) React Native load timeout, default 8000ms

#### `BaseStoryScreenshotTest`

Base test that screenshots all discovered stories. Override:
- `getStoryRendererActivityClass()` - Return your StoryRendererActivity class
- `getLoadTimeoutMs()` - (Optional) Per-story load timeout, default 5000ms
- `shouldScreenshotStory(storyInfo)` - (Optional) Filter which stories to screenshot

## iOS Support

iOS is not yet supported. Contributions welcome!

## License

Apache-2.0
