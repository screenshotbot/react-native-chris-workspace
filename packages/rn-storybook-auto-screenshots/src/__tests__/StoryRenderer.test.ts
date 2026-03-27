jest.mock('react-native', () => ({
  View: 'View',
  Text: 'Text',
  StyleSheet: { create: (s: any) => s },
  NativeModules: { StorybookRegistry: { registerStories: jest.fn() } },
}));

jest.mock('react', () => ({
  useEffect: jest.fn(),
  useState: jest.fn(() => [null, jest.fn()]),
  default: { createElement: jest.fn() },
}));

import { configure, getAllStoryIds, getAllStories } from '../StoryRenderer';

describe('configure', () => {
  beforeEach(() => {
    // Reset by configuring with null
    configure(null as any);
  });

  it('sets the storybook view so other functions can use it', () => {
    const mockView = {
      _storyIndex: { entries: { 'button--primary': { title: 'Button', name: 'Primary' } } },
    };
    configure(mockView);
    expect(getAllStoryIds()).toEqual(['button--primary']);
  });
});

describe('getAllStoryIds', () => {
  beforeEach(() => {
    configure(null as any);
  });

  it('returns empty array when storybookView is not configured', () => {
    expect(getAllStoryIds()).toEqual([]);
  });

  it('returns empty array when _storyIndex is missing', () => {
    configure({});
    expect(getAllStoryIds()).toEqual([]);
  });

  it('returns empty array when entries is empty', () => {
    configure({ _storyIndex: { entries: {} } });
    expect(getAllStoryIds()).toEqual([]);
  });

  it('returns keys from _storyIndex.entries', () => {
    configure({
      _storyIndex: {
        entries: {
          'button--primary': { title: 'Button', name: 'Primary' },
          'card--default': { title: 'Card', name: 'Default' },
        },
      },
    });
    expect(getAllStoryIds()).toEqual(['button--primary', 'card--default']);
  });
});

describe('getAllStories', () => {
  beforeEach(() => {
    configure(null as any);
  });

  it('returns empty array when storybookView is not configured', () => {
    expect(getAllStories()).toEqual([]);
  });

  it('returns empty array when _storyIndex is missing', () => {
    configure({});
    expect(getAllStories()).toEqual([]);
  });

  it('maps entries to {id, title, name} objects', () => {
    configure({
      _storyIndex: {
        entries: {
          'button--primary': { title: 'Button', name: 'Primary' },
          'card--default': { title: 'Card', name: 'Default' },
        },
      },
    });
    expect(getAllStories()).toEqual([
      { id: 'button--primary', title: 'Button', name: 'Primary' },
      { id: 'card--default', title: 'Card', name: 'Default' },
    ]);
  });

  it('ignores extra properties on entries', () => {
    configure({
      _storyIndex: {
        entries: {
          'button--primary': { title: 'Button', name: 'Primary', type: 'story', importPath: './Button.stories' },
        },
      },
    });
    expect(getAllStories()).toEqual([
      { id: 'button--primary', title: 'Button', name: 'Primary' },
    ]);
  });
});
