/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

/**import { NewAppScreen } from '@react-native/new-app-screen';
import { StatusBar, StyleSheet, useColorScheme, View } from 'react-native';
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from 'react-native-safe-area-context';

import MyFeature from './MyFeature';

function App() {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <SafeAreaProvider>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <MyFeature />
      <AppContent />
    </SafeAreaProvider>
  );
}

function AppContent() {
  const safeAreaInsets = useSafeAreaInsets();

  return (
    <View style={styles.container}>
      <NewAppScreen
        templateFileName="App.tsx"
        safeAreaInsets={safeAreaInsets}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});

export default App;*/

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import MyFeature from './MyFeature';
import SwitchFeature from './SwitchFeature';
import { ThemeProvider, ThemeToggle, useTheme } from './Theme';

const AppContent = () => {
  const { colors } = useTheme(); // use the theme colors directly

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.title, { color: colors.text }]}>
        Hello, world!
      </Text>

      <MyFeature />
      <SwitchFeature />

      {/* Theme toggle switch */}
      <ThemeToggle />
    </View>
  );
};

const HelloWorldApp = () => {
  return (
    <ThemeProvider>
      <AppContent />
    </ThemeProvider>
  );
};

export default HelloWorldApp;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  title: {
    fontSize: 22,
    fontWeight: '600',
    marginBottom: 16,
  },
});


