import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-native';
import MyFeature from '../../MyFeature';
import { ThemeProvider } from '../../Theme';

const MyFeatureMeta: Meta<typeof MyFeature> = {
  title: 'MyFeature',
  component: MyFeature,
  decorators: [
    (Story) => (
      <ThemeProvider>
        <Story />
      </ThemeProvider>
    ),
  ],
};

export default MyFeatureMeta;

// Initial state with 0 clicks
const InitialStory = () => {
  const [clickCount, setClickCount] = useState(0);

  return (
    <MyFeature
      clickCount={clickCount}
      setClickCount={setClickCount}
    />
  );
};

export const Initial: StoryObj<typeof MyFeature> = {
  render: () => <InitialStory />,
};

// With some clicks already
const WithClicksStory = () => {
  const [clickCount, setClickCount] = useState(5);

  return (
    <MyFeature
      clickCount={clickCount}
      setClickCount={setClickCount}
    />
  );
};

export const WithClicks: StoryObj<typeof MyFeature> = {
  render: () => <WithClicksStory />,
};

// With many clicks
const ManyClicksStory = () => {
  const [clickCount, setClickCount] = useState(42);

  return (
    <MyFeature
      clickCount={clickCount}
      setClickCount={setClickCount}
    />
  );
};

export const ManyClicks: StoryObj<typeof MyFeature> = {
  render: () => <ManyClicksStory />,
};
