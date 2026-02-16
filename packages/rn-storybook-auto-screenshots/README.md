# rn-storybook-auto-screenshots

Auto-detect Storybook stories and generate screenshot tests for React Native.

Uses [screenshot-tests-for-android](https://github.com/screenshotbot/screenshot-tests-for-android) under the hood to capture screenshots of every Storybook story automatically.

## Installation

```bash
npm install rn-storybook-auto-screenshots
```

## Quick Start

### 1. Configure Storybook

In your app's entry point, configure the library with your Storybook view instance:

```typescript
import { view } from './.rnstorybook/storybook.requires';
import { configure } from 'rn-storybook-auto-screenshots';

configure(view);
```

### 2. Android Setup

#### Add the Screenshotbot plugin to your app's `build.gradle`:

```gradle
plugins {
    id "io.screenshotbot.plugin" version "1.29.11"
}

apply plugin: "io.screenshotbot.screenshot-tests-for-android"

screenshotbot {
    mainBranch "main"
}
```

#### Create a test runner

Create `ScreenshotTestRunner.kt` in your `androidTest` directory:

```kotlin
package com.yourapp

import com.rnstorybookautoscreenshots.BaseScreenshotTestRunner

class ScreenshotTestRunner : BaseScreenshotTestRunner()
```

Then reference it in your `build.gradle`:

```gradle
android {
    defaultConfig {
        testInstrumentationRunner "com.yourapp.ScreenshotTestRunner"
    }
}
```

#### Create a story renderer activity

Create `StoryRendererActivity.kt` in your main source set:

```kotlin
package com.yourapp

import com.rnstorybookautoscreenshots.BaseStoryRendererActivity

class StoryRendererActivity : BaseStoryRendererActivity()
```

Register it in your `AndroidManifest.xml`:

```xml
<activity
    android:name=".StoryRendererActivity"
    android:exported="false" />
```

#### Run screenshot tests

```bash
./gradlew :app:recordAndVerifyDebugScreenshotbotCI
```

## API

### `configure(view)`

Configures the library with your Storybook view instance. Call this once during app initialization.

```typescript
import { view } from './.rnstorybook/storybook.requires';
import { configure } from 'rn-storybook-auto-screenshots';

configure(view);
```

### `<StoryRenderer />`

React component that renders a single Storybook story. Used internally by the screenshot test infrastructure.

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `storyName` | `string` | `"MyFeature/Initial"` | Story to render in `"Title/Name"` format |

```tsx
<StoryRenderer storyName="MyFeature/Initial" />
```

### `getAllStoryIds()`

Returns an array of all registered Storybook story IDs.

```typescript
import { getAllStoryIds } from 'rn-storybook-auto-screenshots';

const ids = getAllStoryIds();
// ["myfeature--initial", "myfeature--loading", ...]
```

### `getAllStories()`

Returns an array of all registered stories with their metadata.

```typescript
import { getAllStories } from 'rn-storybook-auto-screenshots';

const stories = getAllStories();
// [{ id: "myfeature--initial", title: "MyFeature", name: "Initial" }, ...]
```

### `storyNameToId(storyName)`

Converts a story name in `"Title/Name"` format to Storybook's internal ID format.

```typescript
import { storyNameToId } from 'rn-storybook-auto-screenshots';

storyNameToId("MyFeature/Initial"); // "myfeature--initial"
```

## License

Apache-2.0
