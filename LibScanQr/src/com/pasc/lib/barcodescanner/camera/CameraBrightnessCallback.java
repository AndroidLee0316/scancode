package com.pasc.lib.barcodescanner.camera;

/**
 * Created by lanshaomin
 * Date: 2019/7/19 下午3:29
 * Desc:摄像头环境亮度发生变化回调
 */
public interface CameraBrightnessCallback {
  /**
   * 摄像头环境亮度发生变化
   *
   * @param isDark 是否变暗
   */
  void onCameraAmbientBrightnessChanged(boolean isDark);
}
