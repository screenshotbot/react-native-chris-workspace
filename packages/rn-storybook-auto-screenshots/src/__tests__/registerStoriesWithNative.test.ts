const mockRegisterStories = jest.fn();

jest.mock('react-native', () => ({
  View: 'View',
  Text: 'Text',
  StyleSheet: { create: (s: any) => s },
  NativeModules: { StorybookRegistry: { registerStories: mockRegisterStories } },
}));

jest.mock('react', () => ({
  useEffect: jest.fn(),
  useState: jest.fn(() => [null, jest.fn()]),
  default: { createElement: jest.fn() },
}));

function loadFreshModule() {
  let mod: any;
  jest.isolateModules(() => {
    mod = require('../StoryRenderer');
  });
  return mod as typeof import('../StoryRenderer');
}

describe('registerStoriesWithNative', () => {
  beforeEach(() => {
    mockRegisterStories.mockClear();
  });

  it('calls StorybookRegistry.registerStories with story data', () => {
    const { configure, registerStoriesWithNative } = loadFreshModule();
    configure({
      _storyIndex: {
        entries: {
          'button--primary': { title: 'Button', name: 'Primary' },
        },
      },
    });

    registerStoriesWithNative();

    expect(mockRegisterStories).toHaveBeenCalledWith([
      { id: 'button--primary', title: 'Button', name: 'Primary' },
    ]);
  });

  it('does not call registerStories when no stories are available', () => {
    const { configure, registerStoriesWithNative } = loadFreshModule();
    configure({ _storyIndex: { entries: {} } });

    registerStoriesWithNative();

    expect(mockRegisterStories).not.toHaveBeenCalled();
  });

  it('throws when called without configure()', () => {
    const { registerStoriesWithNative } = loadFreshModule();

    expect(() => registerStoriesWithNative()).toThrow('configure()');
  });

  it('only registers once even if called multiple times', () => {
    const { configure, registerStoriesWithNative } = loadFreshModule();
    configure({
      _storyIndex: {
        entries: {
          'button--primary': { title: 'Button', name: 'Primary' },
        },
      },
    });

    registerStoriesWithNative();
    registerStoriesWithNative();
    registerStoriesWithNative();

    expect(mockRegisterStories).toHaveBeenCalledTimes(1);
  });

  it('throws when registerStories throws', () => {
    const { configure, registerStoriesWithNative } = loadFreshModule();
    configure({
      _storyIndex: {
        entries: {
          'button--primary': { title: 'Button', name: 'Primary' },
        },
      },
    });
    mockRegisterStories.mockImplementation(() => {
      throw new Error('native module error');
    });

    expect(() => registerStoriesWithNative()).toThrow('native module error');
  });
});
