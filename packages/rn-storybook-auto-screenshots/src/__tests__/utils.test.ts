import { storyNameToId } from '../utils';

describe('storyNameToId', () => {
  it('converts title/name to lowercase kebab with double dash', () => {
    expect(storyNameToId('MyFeature/Initial')).toBe('myfeature--initial');
  });

  it('handles spaces in title and name', () => {
    expect(storyNameToId('My Feature/Some Story')).toBe('my-feature--some-story');
  });

  it('handles already lowercase input', () => {
    expect(storyNameToId('button/primary')).toBe('button--primary');
  });

  it('handles multiple spaces', () => {
    expect(storyNameToId('My  Feature/My  Story')).toBe('my-feature--my-story');
  });
});
