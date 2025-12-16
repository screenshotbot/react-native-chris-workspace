import React, { useState } from 'react';
import { View, Text, StyleSheet, Button } from 'react-native';
import MyFeature from './MyFeature';
import SwitchFeature from './SwitchFeature';
import TimerFeature from './TimerFeature';
import { ThemeProvider, ThemeToggle, useTheme } from './Theme';

const AppContent = () => {
  const { colors, resetTheme } = useTheme();
  const [clickCount, setClickCount] = useState(0);
  const [isEnabled, setIsEnabled] = useState(false);
  const [timerSeconds, setTimerSeconds] = useState(0);
  const [isTimerRunning, setIsTimerRunning] = useState(false);

  const handleResetAll = () => {
    setClickCount(0);
    setIsEnabled(false);
    setTimerSeconds(0);
    setIsTimerRunning(false);
    resetTheme();
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.title, { color: colors.text }]}>
        Hello, world!
      </Text>

      <MyFeature clickCount={clickCount} setClickCount={setClickCount} />
      <SwitchFeature isEnabled={isEnabled} setIsEnabled={setIsEnabled} />

      <TimerFeature
        seconds={timerSeconds}
        setSeconds={setTimerSeconds}
        isRunning={isTimerRunning}
        setIsRunning={setIsTimerRunning}
      />

      {/* Theme toggle switch */}
      <ThemeToggle />

      {/* Reset button */}
      <View style={styles.resetContainer}>
        <Button
          title="Reset All"
          onPress={handleResetAll}
          color="#FF3B30"
        />
      </View>
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
  resetContainer: {
    marginTop: 30,
    width: 200,
  },
});