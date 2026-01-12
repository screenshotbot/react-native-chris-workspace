import React, { useState } from 'react';
import { View, StyleSheet } from 'react-native';
import MyFeature from './MyFeature';
import { ThemeProvider } from './Theme';

type StoryRendererProps = {
  storyName?: string;
};

/**
 * Component that renders individual stories for screenshot testing.
 * This allows Android tests to render specific story states without
 * launching the full app or Storybook UI.
 */
export default function StoryRenderer({ storyName = 'MyFeature/Initial' }: StoryRendererProps) {
  // Parse the story name (format: "ComponentName/StoryName")
  const [component, story] = storyName.split('/');

  // Determine initial click count based on story
  const getInitialClickCount = () => {
    if (component === 'MyFeature') {
      if (story === 'Initial') return 0;
      if (story === 'WithClicks') return 5;
      if (story === 'ManyClicks') return 42;
    }
    return 0;
  };

  // State must be at top level (Rules of Hooks)
  const [clickCount, setClickCount] = useState(getInitialClickCount());

  // Render the appropriate story based on the name
  const renderStory = () => {
    if (component === 'MyFeature') {
      return <MyFeature clickCount={clickCount} setClickCount={setClickCount} />;
    }

    // Default fallback
    return <MyFeature clickCount={clickCount} setClickCount={setClickCount} />;
  };

  return (
    <ThemeProvider>
      <View style={styles.container}>
        {renderStory()}
      </View>
    </ThemeProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
});
