import AsyncStorage from '@react-native-async-storage/async-storage';
import { view } from '@storybook/react-native';

const StorybookUIRoot = view.getStorybookUI({
  storage: {
    getItem: AsyncStorage.getItem,
    setItem: AsyncStorage.setItem,
  },
});

export { StorybookUIRoot as view };
