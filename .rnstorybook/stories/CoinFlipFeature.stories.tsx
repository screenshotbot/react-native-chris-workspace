import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-native';
import CoinFlipFeature from '../../CoinFlipFeature';
import { ThemeProvider } from '../../Theme';

const CoinFlipFeatureMeta: Meta<typeof CoinFlipFeature> = {
  title: 'CoinFlipFeature',
  component: CoinFlipFeature,
  decorators: [
    (Story) => (
      <ThemeProvider>
        <Story />
      </ThemeProvider>
    ),
  ],
};

export default CoinFlipFeatureMeta;

const CoinFlipFeatureStory = () => {
  const [result, setResult] = useState('');
  const [flips, setFlips] = useState(0);

  return (
    <CoinFlipFeature
      result={result}
      setResult={setResult}
      flips={flips}
      setFlips={setFlips}
    />
  );
};

export const Default: StoryObj<typeof CoinFlipFeature> = {
  render: () => <CoinFlipFeatureStory />,
};

// Story showing the coin after a HEADS result
const HeadsResultStory = () => {
  const [result, setResult] = useState('HEADS');
  const [flips, setFlips] = useState(5);

  return (
    <CoinFlipFeature
      result={result}
      setResult={setResult}
      flips={flips}
      setFlips={setFlips}
    />
  );
};

export const WithHeadsResult: StoryObj<typeof CoinFlipFeature> = {
  render: () => <HeadsResultStory />,
};

// Story showing the coin after a TAILS result
const TailsResultStory = () => {
  const [result, setResult] = useState('TAILS');
  const [flips, setFlips] = useState(10);

  return (
    <CoinFlipFeature
      result={result}
      setResult={setResult}
      flips={flips}
      setFlips={setFlips}
    />
  );
};

export const WithTailsResult: StoryObj<typeof CoinFlipFeature> = {
  render: () => <TailsResultStory />,
};
