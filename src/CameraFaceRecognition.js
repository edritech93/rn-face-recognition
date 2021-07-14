import React from 'react';
import PropTypes from 'prop-types';
import {
    findNodeHandle,
    Platform,
    NativeModules,
    ViewPropTypes,
    requireNativeComponent,
    View,
    ActivityIndicator,
    Text,
    StyleSheet,
    PermissionsAndroid,
} from 'react-native';

const CameraManager = NativeModules.CameraModule ||
    NativeModules.RNCameraModule || {
    stubbed: true,
    Type: {
        back: 1,
    },
    AutoFocus: {
        on: 1,
    },
    FlashMode: {
        off: 1,
    },
    WhiteBalance: {},
    BarCodeType: {},
    FaceDetection: {
        fast: 1,
        Mode: {},
        Landmarks: {
            none: 0,
        },
        Classifications: {
            none: 0,
        },
    },
    GoogleVisionBarcodeDetection: {
        BarcodeType: 0,
        BarcodeMode: 0,
    },
};

export const Constants = Camera.Constants;

const CameraModule = requireNativeComponent('CameraModule', Camera, {
    nativeOnly: {
        accessibilityComponentType: true,
        accessibilityLabel: true,
        accessibilityLiveRegion: true,
        barCodeScannerEnabled: true,
        touchDetectorEnabled: true,
        googleVisionBarcodeDetectorEnabled: true,
        faceDetectorEnabled: true,
        textRecognizerEnabled: true,
        importantForAccessibility: true,
        onBarCodeRead: true,
        onGoogleVisionBarcodesDetected: true,
        onCameraReady: true,
        onAudioInterrupted: true,
        onAudioConnected: true,
        onPictureSaved: true,
        onFaceDetected: true,
        onTouch: true,
        onLayout: true,
        onMountError: true,
        onSubjectAreaChanged: true,
        renderToHardwareTextureAndroid: true,
        testID: true,
    },
});

export default class CameraModule extends React.Component {

    async takePictureAsync(options) {
        return await CameraManager.takePicture(options, this._cameraHandle);
    }

    _setReference = (ref) => {
        if (ref) {
            this._cameraRef = ref;
            this._cameraHandle = findNodeHandle(ref);
        } else {
            this._cameraRef = null;
            this._cameraHandle = null;
        }
    };

    render() {
        return (
            <CameraModule
                style={StyleSheet.absoluteFill}
                ref={this._setReference}
            />
        );
    }
}
