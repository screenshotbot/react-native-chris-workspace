import React, { createContext, useContext, useState, ReactNode } from 'react';
import { View, Text, Switch, StyleSheet } from 'react-native';

// Types
export type Theme = 'light' | 'dark';

type ThemeColors = {
  background: string;
  text: string;
  card: string;
  primary: string;
};

type ThemeContextType = {
  theme: Theme;
  colors: ThemeColors;
  toggleTheme: () => void;
  resetTheme: () => void;
};

// Colors
const lightColors: ThemeColors = {
  background: '#ffffff',
  text: '#000000',
  card: '#f0f0f0',
  primary: '#007AFF',
};

const darkColors: ThemeColors = {
  background: '#1c1c1e',
  text: '#ffffff',
  card: '#2c2c2e',
  primary: '#4DA3FF',
};

// Context
const ThemeContext = createContext<ThemeContextType>({
  theme: 'light',
  colors: lightColors,
  toggleTheme: () => {},
  resetTheme: () => {},
});

// Provider
export const ThemeProvider = ({ children }: { children: ReactNode }) => {
  const [theme, setTheme] = useState<Theme>('light');

  const toggleTheme = () => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  };

  const resetTheme = () => {
    setTheme('light');
  };

  const colors = theme === 'light' ? lightColors : darkColors;

  return (
    <ThemeContext.Provider value={{ theme, colors, toggleTheme, resetTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

// Hook
export const useTheme = () => useContext(ThemeContext);

// Theme toggle UI
export const ThemeToggle = () => {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <View style={styles.toggleContainer}>
      <Text style={{ color: isDark ? '#fff' : '#000', marginRight: 8 }}>
        {theme.toUpperCase()} MODE
      </Text>
      <Switch value={isDark} onValueChange={toggleTheme} />
    </View>
  );
};

const styles = StyleSheet.create({
  toggleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 20,
  },
});