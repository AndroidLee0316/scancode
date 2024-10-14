/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pasc.lib.barcodescanner.camera;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.pasc.lib.zxing.client.android.AmbientLightManager;
import com.pasc.lib.zxing.client.android.camera.CameraConfigurationUtils;
import com.pasc.lib.zxing.client.android.camera.open.OpenCameraInterface;
import com.pasc.lib.barcodescanner.Size;
import com.pasc.lib.barcodescanner.SourceData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper to manage the Camera. This is not thread-safe, and the methods must always be called
 * from the same thread.
 *
 *
 * Call order:
 *
 * 1. setCameraSettings()
 * 2. open(), set desired preview size (any order)
 * 3. configure(), setPreviewDisplay(holder) (any order)
 * 4. startPreview()
 * 5. requestPreviewFrame (repeat)
 * 6. stopPreview()
 * 7. close()
 */
public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();

  private Camera camera;
  private Camera.CameraInfo cameraInfo;

  private AutoFocusManager autoFocusManager;
  private AmbientLightManager ambientLightManager;

  private boolean previewing;
  private String defaultParameters;

  // User parameters
  private CameraSettings settings = new CameraSettings();

  private DisplayConfiguration displayConfiguration;

  // Actual chosen preview size
  private Size requestedPreviewSize;
  private Size previewSize;

  private int rotationDegrees = -1;    // camera rotation vs display rotation

  private Context context;
  /**
   * 上次环境亮度记录的时间戳
   */
  private long mLastAmbientBrightnessRecordTime = System.currentTimeMillis();
  /**
   * 上次环境亮度记录的索引
   */
  private int mAmbientBrightnessDarkIndex = 0;
  /**
   * 环境亮度历史记录的数组，255 是代表亮度最大值
   */
  private static final long[] AMBIENT_BRIGHTNESS_DARK_LIST = new long[] { 255, 255, 255, 255 };
  /**
   * 环境亮度扫描间隔
   */
  private static final int AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME = 150;
  /**
   * 亮度低的阀值
   */
  private static final int AMBIENT_BRIGHTNESS_DARK = 60;

  protected CameraBrightnessCallback mDelegate;


  private final class CameraPreviewCallback implements Camera.PreviewCallback {
    private PreviewCallback callback;

    private Size resolution;

    public CameraPreviewCallback() {
    }

    public void setResolution(Size resolution) {
      this.resolution = resolution;
    }

    public void setCallback(PreviewCallback callback) {
      this.callback = callback;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
      //            Log.e("time_to", "onPreviewFrame: - >" );
      if (camera != null && previewing) {
        try {
          handleAmbientBrightness(data, camera);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      Size cameraResolution = resolution;
      PreviewCallback callback = this.callback;
      if (cameraResolution != null && callback != null) {
        int format = camera.getParameters().getPreviewFormat();
        SourceData source = new SourceData(data, cameraResolution.width, cameraResolution.height, format, getCameraRotation());
        callback.onPreview(source);
      } else {
        Log.d(TAG, "Got preview callback, but no handler or resolution available");
      }
    }
  }

  /**
   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
   * clear the handler so it will only receive one message.
   */
  private final CameraPreviewCallback cameraPreviewCallback;

  public CameraManager(Context context) {
    this.context = context;
    cameraPreviewCallback = new CameraPreviewCallback();
  }

  /**
   * Must be called from camera thread.
   */
  public void open() {
    camera = OpenCameraInterface.open(settings.getRequestedCameraId());
    if (camera == null) {
      throw new RuntimeException("Failed to open camera");
    }

    int cameraId = OpenCameraInterface.getCameraId(settings.getRequestedCameraId());
    cameraInfo = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, cameraInfo);
  }

  /**
   * Configure the camera parameters, including preview size.
   *
   * The camera must be opened before calling this.
   *
   * Must be called from camera thread.
   */
  public void configure() {
    if(camera == null) {
      throw new RuntimeException("Camera not open");
    }
    setParameters();
  }

  /**
   * Must be called from camera thread.
   */
  public void setPreviewDisplay(SurfaceHolder holder) throws IOException {
    setPreviewDisplay(new CameraSurface(holder));
  }

  public void setPreviewDisplay(CameraSurface surface) throws IOException {
    surface.setPreview(camera);
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   *
   * Must be called from camera thread.
   */
  public void startPreview() {
    Camera theCamera = camera;
    if (theCamera != null && !previewing) {
      theCamera.startPreview();
      previewing = true;
      autoFocusManager = new AutoFocusManager(camera, settings);
      ambientLightManager = new AmbientLightManager(context, this, settings);
      ambientLightManager.start();
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   *
   * Must be called from camera thread.
   */
  public void stopPreview() {
    if (autoFocusManager != null) {
      autoFocusManager.stop();
      autoFocusManager = null;
    }
    if (ambientLightManager != null) {
      ambientLightManager.stop();
      ambientLightManager = null;
    }
    if (camera != null && previewing) {
      camera.stopPreview();
      cameraPreviewCallback.setCallback(null);
      previewing = false;
    }
  }


  /**
   * Closes the camera driver if still in use.
   *
   * Must be called from camera thread.
   */
  public void close() {
    if (camera != null) {
      camera.release();
      camera = null;
    }
  }

  /**
   * @return true if the camera rotation is perpendicular to the current display rotation.
   */
  public boolean isCameraRotated() {
    if(rotationDegrees == -1) {
      throw new IllegalStateException("Rotation not calculated yet. Call configure() first.");
    }
    return rotationDegrees % 180 != 0;
  }

  /**
   *
   * @return the camera rotation relative to display rotation, in degrees. Typically 0 if the
   *    display is in landscape orientation.
   */
  public int getCameraRotation() {
    return rotationDegrees;
  }


  private Camera.Parameters getDefaultCameraParameters() {
    Camera.Parameters parameters = camera.getParameters();
    if (defaultParameters == null) {
      defaultParameters = parameters.flatten();
    } else {
      parameters.unflatten(defaultParameters);
    }
    return parameters;
  }

  private void setDesiredParameters(boolean safeMode) {
    Camera.Parameters parameters = getDefaultCameraParameters();

    //noinspection ConstantConditions
    if (parameters == null) {
      Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
      return;
    }

    Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

    if (safeMode) {
      Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
    }


    CameraConfigurationUtils.setFocus(parameters, settings.getFocusMode(), safeMode);

    if (!safeMode) {
      CameraConfigurationUtils.setTorch(parameters, false);

      if (settings.isScanInverted()) {
        CameraConfigurationUtils.setInvertColor(parameters);
      }

      if (settings.isBarcodeSceneModeEnabled()) {
        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
      }

      if (settings.isMeteringEnabled()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
          CameraConfigurationUtils.setVideoStabilization(parameters);
          CameraConfigurationUtils.setFocusArea(parameters);
          CameraConfigurationUtils.setMetering(parameters);
        }
      }

    }

    List<Size> previewSizes = getPreviewSizes(parameters);
    if (previewSizes.size() == 0) {
      requestedPreviewSize = null;
    } else {
      requestedPreviewSize = displayConfiguration.getBestPreviewSize(previewSizes, isCameraRotated());

      parameters.setPreviewSize(requestedPreviewSize.width, requestedPreviewSize.height);
    }

    if (Build.DEVICE.equals("glass-1")) {
      // We need to set the FPS on Google Glass devices, otherwise the preview is scrambled.
      // FIXME - can/should we do this for other devices as well?
      CameraConfigurationUtils.setBestPreviewFPS(parameters);
    }

    Log.i(TAG, "Final camera parameters: " + parameters.flatten());

    camera.setParameters(parameters);
  }

  private static List<Size> getPreviewSizes(Camera.Parameters parameters) {
    List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
    List<Size> previewSizes = new ArrayList<>();
    if (rawSupportedSizes == null) {
      Camera.Size defaultSize = parameters.getPreviewSize();
      if (defaultSize != null) {
        // Work around potential platform bugs
        previewSizes.add(new Size(defaultSize.width, defaultSize.height));
      }
      return previewSizes;
    }
    for (Camera.Size size : rawSupportedSizes) {
      previewSizes.add(new Size(size.width, size.height));
    }
    return previewSizes;
  }

  private int calculateDisplayRotation() {
    // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
    int rotation = displayConfiguration.getRotation();
    int degrees = 0;
    switch (rotation) {
      case Surface.ROTATION_0:
        degrees = 0;
        break;
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_180:
        degrees = 180;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
    }

    int result;
    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (cameraInfo.orientation + degrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror
    } else {  // back-facing
      result = (cameraInfo.orientation - degrees + 360) % 360;
    }
    Log.i(TAG, "Camera Display Orientation: " + result);
    return result;
  }

  private void setCameraDisplayOrientation(int rotation) {
    camera.setDisplayOrientation(rotation);
  }


  private void setParameters() {
    try {
      this.rotationDegrees = calculateDisplayRotation();
      setCameraDisplayOrientation(rotationDegrees);
    } catch (Exception e) {
      Log.w(TAG, "Failed to set rotation.");
    }
    try {
      setDesiredParameters(false);
    } catch (Exception e) {
      // Failed, use safe mode
      try {
        setDesiredParameters(true);
      } catch (Exception e2) {
        // Well, darn. Give up
        Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
      }
    }

    Camera.Size realPreviewSize = camera.getParameters().getPreviewSize();
    if (realPreviewSize == null) {
      previewSize = requestedPreviewSize;
    } else {
      previewSize = new Size(realPreviewSize.width, realPreviewSize.height);
    }
    cameraPreviewCallback.setResolution(previewSize);
  }

  /**
   * This returns false if the camera is not opened yet, failed to open, or has
   * been closed.
   */
  public boolean isOpen() {
    return camera != null;
  }

  /**
   * Actual preview size in *natural camera* orientation. null if not determined yet.
   *
   * @return preview size
   */
  public Size getNaturalPreviewSize() {
    return previewSize;
  }

  /**
   * Actual preview size in *current display* rotation. null if not determined yet.
   *
   * @return preview size
   */
  public Size getPreviewSize() {
    if (previewSize == null) {
      return null;
    } else if (this.isCameraRotated()) {
      return previewSize.rotate();
    } else {
      return previewSize;
    }
  }

  /**
   * A single preview frame will be returned to the supplied callback.
   *
   * The thread on which this called is undefined, so a Handler should be used to post the result
   * to the correct thread.
   *
   * @param callback The callback to receive the preview.
   */
  public void requestPreviewFrame(PreviewCallback callback) {
    Camera theCamera = camera;
    if (theCamera != null && previewing) {
      cameraPreviewCallback.setCallback(callback);
      theCamera.setOneShotPreviewCallback(cameraPreviewCallback);
    }
  }

  public CameraSettings getCameraSettings() {
    return settings;
  }

  public void setCameraSettings(CameraSettings settings) {
    this.settings = settings;
  }

  public DisplayConfiguration getDisplayConfiguration() {
    return displayConfiguration;
  }

  public void setDisplayConfiguration(DisplayConfiguration displayConfiguration) {
    this.displayConfiguration = displayConfiguration;
  }

  public void setTorch(boolean on) {
    if (camera != null) {
      boolean isOn = isTorchOn();
      if (on != isOn) {
        if (autoFocusManager != null) {
          autoFocusManager.stop();
        }

        Camera.Parameters parameters = camera.getParameters();
        CameraConfigurationUtils.setTorch(parameters, on);
        if (settings.isExposureEnabled()) {
          CameraConfigurationUtils.setBestExposure(parameters, on);
        }
        camera.setParameters(parameters);

        if (autoFocusManager != null) {
          autoFocusManager.start();
        }
      }
    }
  }

  public boolean isTorchOn() {
    Camera.Parameters parameters = camera.getParameters();
    if (parameters != null) {
      String flashMode = parameters.getFlashMode();
      return flashMode != null &&
          (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
              Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
    } else {
      return false;
    }
  }

  /**
   * Returns the Camera. This returns null if the camera is not opened yet, failed to open, or has
   * been closed.
   *
   * @return the Camera
   */
  public Camera getCamera() {
    return camera;
  }

  private void handleAmbientBrightness(byte[] data, Camera camera) {
    if(data==null){
      return;
    }
    long currentTime = System.currentTimeMillis();
    if (currentTime - mLastAmbientBrightnessRecordTime < AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME) {
      return;
    }
    mLastAmbientBrightnessRecordTime = currentTime;

    int width = camera.getParameters().getPreviewSize().width;
    int height = camera.getParameters().getPreviewSize().height;
    // 像素点的总亮度
    long pixelLightCount = 0L;
    // 像素点的总数
    long pixelCount = width * height;
    // 采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于等于1。
    int step = 10;
    // data.length - allCount * 1.5f 的目的是判断图像格式是不是 YUV420 格式，只有是这种格式才相等
    //因为 int 整形与 float 浮点直接比较会出问题，所以这么比
    if (Math.abs(data.length - pixelCount * 1.5f) < 0.00001f) {
      for (int i = 0; i < pixelCount; i += step) {
        // 如果直接加是不行的，因为 data[i] 记录的是色值并不是数值，byte 的范围是 +127 到 —128，
        // 而亮度 FFFFFF 是 11111111 是 -127，所以这里需要先转为无符号 unsigned long 参考 Byte.toUnsignedLong()
        pixelLightCount += ((long) data[i]) & 0xffL;
      }
      // 平均亮度
      long cameraLight = pixelLightCount / (pixelCount / step);
      // 更新历史记录
      int lightSize = AMBIENT_BRIGHTNESS_DARK_LIST.length;
      AMBIENT_BRIGHTNESS_DARK_LIST[mAmbientBrightnessDarkIndex =
          mAmbientBrightnessDarkIndex % lightSize] = cameraLight;
      mAmbientBrightnessDarkIndex++;
      boolean isDarkEnv = true;
      // 判断在时间范围 AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME * lightSize 内是不是亮度过暗
      for (long ambientBrightness : AMBIENT_BRIGHTNESS_DARK_LIST) {
        if (ambientBrightness > AMBIENT_BRIGHTNESS_DARK) {
          isDarkEnv = false;
          break;
        }
      }
      //Log.d("摄像头环境亮度为：", "" + cameraLight);
      if (mDelegate != null) {
        mDelegate.onCameraAmbientBrightnessChanged(isDarkEnv);
      }
    }
  }

  public void setDelegate(CameraBrightnessCallback mDelegate) {
    this.mDelegate = mDelegate;
  }
}
