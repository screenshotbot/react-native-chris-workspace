import React, { useState } from 'react';
import { View, StyleSheet } from 'react-native';
import MyFeature from './MyFeature';
import { ThemeProvider } from './Theme';

type StoryRendererProps = {
  storyName?: string;
};

/**
 * Renders individual Storybook stories for screenshot testing.
 * Format: "ComponentName/StoryName" (e.g., "MyFeature/Initial")
 */
export default function StoryRenderer({ storyName = 'MyFeature/Initial' }: StoryRendererProps) {
  const [component, story] = storyName.split('/');

  // Determine initial state based on story name
  const getInitialState = () => {
    if (component === 'MyFeature') {
      if (story === 'Initial') return 0;
      if (story === 'WithClicks') return 5;
      if (story === 'ManyClicks') return 42;
    }
    return 0;
  };

  const [clickCount, setClickCount] = useState(getInitialState);

  return (
    <ThemeProvider>
      <View style={styles.container}>
        <MyFeature clickCount={clickCount} setClickCount={setClickCount} />
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
