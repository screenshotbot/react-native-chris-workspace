import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-native';
import TimerFeature from '../../TimerFeature';
import { ThemeProvider } from '../../Theme';

const TimerFeatureMeta: Meta<typeof TimerFeature> = {
  title: 'TimerFeature',
  component: TimerFeature,
  decorators: [
    (Story) => (
      <ThemeProvider>
        <Story />
      </ThemeProvider>
    ),
  ],
};

export default TimerFeatureMeta;

// Timer at 0, not running
const InitialStory = () => {
  const [seconds, setSeconds] = useState(0);
  const [isRunning, setIsRunning] = useState(false);

  return (
    <TimerFeature
      seconds={seconds}
      setSeconds={setSeconds}
      isRunning={isRunning}
      setIsRunning={setIsRunning}
    />
  );
};

export const Initial: StoryObj<typeof TimerFeature> = {
  render: () => <InitialStory />,
};

// Timer running
const RunningStory = () => {
  const [seconds, setSeconds] = useState(15);
  const [isRunning, setIsRunning] = useState(true);

  return (
    <TimerFeature
      seconds={seconds}
      setSeconds={setSeconds}
      isRunning={isRunning}
      setIsRunning={setIsRunning}
    />
  );
};

export const Running: StoryObj<typeof TimerFeature> = {
  render: () => <RunningStory />,
};

// Timer paused with some time
const PausedStory = () => {
  const [seconds, setSeconds] = useState(125);
  const [isRunning, setIsRunning] = useState(false);

  return (
    <TimerFeature
      seconds={seconds}
      setSeconds={setSeconds}
      isRunning={isRunning}
      setIsRunning={setIsRunning}
    />
  );
};

export const Paused: StoryObj<typeof TimerFeature> = {
  render: () => <PausedStory />,
};

// Timer showing hours, minutes, seconds
const LongDurationStory = () => {
  const [seconds, setSeconds] = useState(3661); // 1 hour, 1 minute, 1 second
  const [isRunning, setIsRunning] = useState(false);

  return (
    <TimerFeature
      seconds={seconds}
      setSeconds={setSeconds}
      isRunning={isRunning}
      setIsRunning={setIsRunning}
    />
  );
};

export const LongDuration: StoryObj<typeof TimerFeature> = {
  render: () => <LongDurationStory />,
};
