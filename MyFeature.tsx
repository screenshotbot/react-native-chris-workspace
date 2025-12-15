import React from 'react';
import { View, Text, Button, StyleSheet, Alert } from 'react-native';
import { useTheme } from './Theme';

type MyFeatureProps = {
  clickCount: number;
  setClickCount: React.Dispatch<React.SetStateAction<number>>;
};

export default function MyFeature({ clickCount, setClickCount }: MyFeatureProps) {
  const { colors } = useTheme();

  const handlePress = () => {
    setClickCount(prev => prev + 1);
    Alert.alert(
      'Feature clicked!',
      `You have clicked ${clickCount + 1} times.`
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.card }]}>
      <Text style={[styles.title, { color: colors.text }]}>
        My Feature
      </Text>

      <Button
        title="Click Me!"
        onPress={handlePress}
        color={colors.primary}
      />

      <Text style={[styles.counter, { color: colors.text }]}>
        Clicked {clickCount} times
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 20,
    alignItems: 'center',
    marginTop: 20,
    borderRadius: 10,
  },
  title: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 10,
  },
  counter: {
    fontSize: 16,
    marginTop: 10,
  },
});