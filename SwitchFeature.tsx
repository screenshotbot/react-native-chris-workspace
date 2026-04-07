import React from 'react';
import { View, Text, Switch, StyleSheet } from 'react-native';
import { useTheme } from './Theme';

type SwitchFeatureProps = {
  isEnabled: boolean;
  setIsEnabled: React.Dispatch<React.SetStateAction<boolean>>;
};

export default function SwitchFeature({ isEnabled, setIsEnabled }: SwitchFeatureProps) {
  const { colors } = useTheme();

  const toggleSwitch = () => setIsEnabled(prev => !prev);

  return (
    <View style={[styles.container, { backgroundColor: colors.card }]}>
      <Text style={[styles.label, { color: colors.text }]}>
        Feature Switch
      </Text>

      {/* Switch */}
      <Switch
        trackColor={{ false: '#767577', true: colors.primary }}
        thumbColor={isEnabled ? colors.primary : '#f4f3f4'}
        ios_backgroundColor="#3e3e3e"
        onValueChange={toggleSwitch}
        value={isEnabled}
      />

      <Text style={[styles.status, { color: colors.text }]}>
        {isEnabled ? '' : 'False'}
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
  label: {
    fontSize: 18,
    fontWeight: '500',
    marginBottom: 10,
  },
  status: {
    marginTop: 10,
    fontSize: 16,
  },
});