import 'dart:ui';

import 'package:mobile_scanner/src/enums/camera_facing.dart';
import 'package:mobile_scanner/src/enums/torch_state.dart';
import 'package:mobile_scanner/src/mobile_scanner_exception.dart';

/// This class represents the current state of a [MobileScannerController].
class MobileScannerState {
  /// Create a new [MobileScannerState] instance.
  const MobileScannerState({
    required this.cameraDirection,
    required this.isInitialized,
    required this.size,
    required this.torchState,
    required this.zoomScale,
    this.error,
  });

  /// Create a new [MobileScannerState] instance that is uninitialized.
  const MobileScannerState.uninitialized(CameraFacing facing)
      : this(
          cameraDirection: facing,
          isInitialized: false,
          size: Size.zero,
          torchState: TorchState.unavailable,
          zoomScale: 1.0,
        );

  /// The facing direction of the camera.
  final CameraFacing cameraDirection;

  /// The error that occurred while setting up or using the canera.
  final MobileScannerException? error;

  /// Whether the mobile scanner has initialized successfully.
  final bool isInitialized;

  /// The size of the camera output.
  final Size size;

  /// The current state of the flashlight of the camera.
  final TorchState torchState;

  /// The current zoom scale of the camera.
  final double zoomScale;

  /// Create a copy of this state with the given parameters.
  MobileScannerState copyWith({
    CameraFacing? cameraDirection,
    MobileScannerException? error,
    bool? isInitialized,
    Size? size,
    TorchState? torchState,
    double? zoomScale,
  }) {
    return MobileScannerState(
      cameraDirection: cameraDirection ?? this.cameraDirection,
      error: error,
      isInitialized: isInitialized ?? this.isInitialized,
      size: size ?? this.size,
      torchState: torchState ?? this.torchState,
      zoomScale: zoomScale ?? this.zoomScale,
    );
  }
}
