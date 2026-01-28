# rn-storybook-auto-screenshots

Auto-detect Storybook stories and generate screenshot tests for React Native.

## Installation

```bash
npm install rn-storybook-auto-screenshots
# or
yarn add rn-storybook-auto-screenshots
```

## Setup

### 1. Configure in your app

```typescript
// In your app initialization (e.g., index.js or App.tsx)
import { view } from './.rnstorybook/storybook.requires';
import { configure } from 'rn-storybook-auto-screenshots';

configure(view);
```

### 2. Create StoryRendererActivity

Create an Activity that renders stories for screenshot testing:

```kotlin
// android/app/src/main/java/com/yourapp/StoryRendererActivity.kt
class StoryRendererActivity : ReactActivity() {
    companion object {
        const val EXTRA_STORY_NAME = "story_name"
    }

    override fun getMainComponentName(): String = "StoryRenderer"

    override fun createReactActivityDelegate(): ReactActivityDelegate {
        return object : ReactActivityDelegate(this, mainComponentName) {
            override fun getLaunchOptions(): Bundle? {
                return Bundle().apply {
                    putString("storyName", intent.getStringExtra(EXTRA_STORY_NAME) ?: "")
                }
            }
        }
    }
}
```

### 3. Register the component

```typescript
import { AppRegistry } from 'react-native';
import { StoryRenderer } from 'rn-storybook-auto-screenshots';

AppRegistry.registerComponent('StoryRenderer', () => StoryRenderer);
```

### 4. Add screenshot tests

See the example test in the repository for how to create screenshot tests that auto-discover stories.

## API

### `configure(view)`

Configure the library with your Storybook view instance.

### `StoryRenderer`

React component that renders a story by name.

### `getAllStories()`

Returns an array of all stories with their metadata.

### `getAllStoryIds()`

Returns an array of all story IDs.

## License

Apache-2.0
