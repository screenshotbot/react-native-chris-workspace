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

  it('does not call registerStories when storybookView is not configured', () => {
    const { registerStoriesWithNative } = loadFreshModule();

    registerStoriesWithNative();

    expect(mockRegisterStories).not.toHaveBeenCalled();
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

  it('logs a warning and does not throw when registerStories throws', () => {
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

    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

    expect(() => registerStoriesWithNative()).not.toThrow();
    expect(warnSpy).toHaveBeenCalled();

    warnSpy.mockRestore();
  });
});
