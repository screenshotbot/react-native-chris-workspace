# rn-storybook-auto-screenshots

Auto-detect Storybook stories and generate screenshot tests for React Native.

## Installation

```bash
npm install rn-storybook-auto-screenshots
```

## Quick Start

```typescript
import { view } from './.rnstorybook/storybook.requires';
import { configure, StoryRenderer } from 'rn-storybook-auto-screenshots';

// Configure with your Storybook view
configure(view);

// Render a story
<StoryRenderer storyName="MyFeature/Initial" />
```

## License

Apache-2.0
