import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated } from 'react-native';
import { useTheme } from './Theme';

type CoinFlipFeatureProps = {
  result: string;
  setResult: React.Dispatch<React.SetStateAction<string>>;
  flips: number;
  setFlips: React.Dispatch<React.SetStateAction<number>>;
};

export default function CoinFlipFeature({
  result,
  setResult,
  flips,
  setFlips,
}: CoinFlipFeatureProps) {
  const { colors } = useTheme();
  const [isFlipping, setIsFlipping] = useState(false);
  const [rotateAnim] = useState(new Animated.Value(0));

  const flipCoin = () => {
    if (isFlipping) return;

    setIsFlipping(true);
    setResult('');

    // Reset rotation
    rotateAnim.setValue(0);

    // Animate rotation
    Animated.timing(rotateAnim, {
      toValue: 1,
      duration: 1000,
      useNativeDriver: true,
    }).start(() => {
      // Randomly determine result
      const coinResult = Math.random() < 0.5 ? 'HEADS' : 'TAILS';
      setResult(coinResult);
      setFlips(prev => prev + 1);
      setIsFlipping(false);
    });
  };

  const spin = rotateAnim.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '1080deg'], // 3 full rotations
  });

  return (
    <View style={[styles.container, { backgroundColor: colors.card }]}>
      <Text style={[styles.title, { color: colors.text }]}>Coin Flip</Text>

      <Animated.View
        style={[
          styles.coin,
          {
            backgroundColor: result === 'HEADS' ? '#FFD700' : result === 'TAILS' ? '#C0C0C0' : colors.primary,
            transform: [{ rotateY: spin }],
          },
        ]}>
        <Text style={styles.coinText}>
          {isFlipping ? '?' : result || '¢'}
        </Text>
      </Animated.View>

      <TouchableOpacity
        style={[
          styles.button,
          { backgroundColor: colors.primary },
          isFlipping && styles.buttonDisabled,
        ]}
        onPress={flipCoin}
        disabled={isFlipping}>
        <Text style={styles.buttonText}>
          {isFlipping ? 'Flipping...' : 'Flip Coin'}
        </Text>
      </TouchableOpacity>

      <View style={styles.statsContainer}>
        <Text style={[styles.statsText, { color: colors.text }]}>
          Total Flips: {flips}
        </Text>
        {result && (
          <Text style={[styles.resultText, { color: colors.text }]}>
            Result: {result}
          </Text>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 20,
    alignItems: 'center',
    marginTop: 20,
    borderRadius: 10,
    width: '90%',
  },
  title: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 20,
  },
  coin: {
    width: 120,
    height: 120,
    borderRadius: 60,
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 5,
    elevation: 8,
  },
  coinText: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#ffffff',
  },
  button: {
    paddingHorizontal: 40,
    paddingVertical: 15,
    borderRadius: 8,
    marginTop: 10,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  statsContainer: {
    marginTop: 20,
    alignItems: 'center',
  },
  statsText: {
    fontSize: 16,
    marginBottom: 5,
  },
  resultText: {
    fontSize: 18,
    fontWeight: '600',
  },
});