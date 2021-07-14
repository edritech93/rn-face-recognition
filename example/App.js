import React, { Component } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { CameraFaceRecognition } from '../src';

export default function App(props) {
  return (
    <View style={styles.container}>
      <Text> App </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'white'
  }
})
