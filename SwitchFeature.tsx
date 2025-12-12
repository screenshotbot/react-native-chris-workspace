import React, { useState } from 'react';
import { View, Text, Switch, StyleSheet, Image } from 'react-native';
import { useTheme } from './Theme';

export default function SwitchFeature() {
  const [isEnabled, setIsEnabled] = useState(false);
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

      {/* Conditional image */}
      {isEnabled && (
        <Image
          source={{
            uri: 'https://preview.redd.it/morgan-freeman-true-3700-x-3700-v0-g0fa2kf347391.png?width=320&crop=smart&auto=webp&s=052771a34f448167b4c62c696dcd321b9e15cccb',
          }}
          style={styles.image}
        />
      )}
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
  image: {
    marginTop: 20,
    width: 200,
    height: 200,
    resizeMode: 'contain',
    borderRadius: 10,
  },
});
