import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-native';
import SwitchFeature from '../../SwitchFeature';
import { ThemeProvider } from '../../Theme';

const SwitchFeatureMeta: Meta<typeof SwitchFeature> = {
  title: 'SwitchFeature',
  component: SwitchFeature,
  decorators: [
    (Story) => (
      <ThemeProvider>
        <Story />
      </ThemeProvider>
    ),
  ],
};

export default SwitchFeatureMeta;

// Switch in OFF state
const SwitchOffStory = () => {
  const [isEnabled, setIsEnabled] = useState(false);

  return (
    <SwitchFeature
      isEnabled={isEnabled}
      setIsEnabled={setIsEnabled}
    />
  );
};

export const SwitchOff: StoryObj<typeof SwitchFeature> = {
  render: () => <SwitchOffStory />,
};

// Switch in ON state
const SwitchOnStory = () => {
  const [isEnabled, setIsEnabled] = useState(true);

  return (
    <SwitchFeature
      isEnabled={isEnabled}
      setIsEnabled={setIsEnabled}
    />
  );
};

export const SwitchOn: StoryObj<typeof SwitchFeature> = {
  render: () => <SwitchOnStory />,
};
