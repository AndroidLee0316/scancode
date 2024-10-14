package com.pasc.lib.scanqr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.view.View;
import com.pasc.lib.barcodescanner.BarCodeInfoCallback;

/**
 * Created by ex-huangzhiyi001 on 2019/5/10.
 */
public class ScanQrManager {
  /**
   * 结果由h5处理
   */
  public static final int SCAN_RESULT_H5=1;
  /**
   * 结果由业务处理
   */
  public static final int SCAN_RESULT_NATIVITE=2;
  /**
   * 扫码页面默认
   */
  public static final int SCAN_RESULT_CURRENT_PAGE = 1;
  /**
   * 前一页面处理
   */
  public static final int SCAN_RESULT_PRE_PAGE = 2;
  public static final int SCAN_QR_CODE = 24;
  private int decoratedBarcodeViewId = 0;
  private ViewCallback viewCallback;
  private String url;
  /**
   * 接口回调
   */
  private BarCodeInfoCallback barCodeInfoCallback;
  /**
   * 结果 1：扫码页面默认 2：前一页面处理
   */
  private int resultProcessing = SCAN_RESULT_CURRENT_PAGE;
  /**
   * 默认由h5处理
   */
  private int scanResult=SCAN_RESULT_H5;

  private static class SingletonHolder {
    private static final ScanQrManager INSTANCE = new ScanQrManager();
  }

  public static ScanQrManager getInstance() {
    return ScanQrManager.SingletonHolder.INSTANCE;
  }

  private ScanQrManager() {

  }

  /**
   * 跳转
   */
  public void startScan(Activity activity) {
    startScan(activity, null);
  }

  /**
   * 跳转
   */
  public void startScan(Activity activity, Bundle bundle) {
    if (decoratedBarcodeViewId == 0 || viewCallback == null) {
      Intent intent = new Intent(activity, CustomStandardCaptureActivity.class);
      if (bundle != null) {
        intent.putExtras(bundle);
      }
      activity.startActivityForResult(intent, SCAN_QR_CODE);
    } else {
      Intent intent = new Intent(activity, CustomCaptureActivity.class);
      if (bundle != null) {
        intent.putExtras(bundle);
      }
      activity.startActivityForResult(intent, SCAN_QR_CODE);
    }
  }

  /**
   * DecoratedBarcodeView id,
   */
  public void setContentView(@LayoutRes int decoratedBarcodeViewId) {
    this.decoratedBarcodeViewId = decoratedBarcodeViewId;
  }

  public void setViewCallback(ViewCallback viewCallback) {
    this.viewCallback = viewCallback;
  }

  public interface ViewCallback {
    void initView(CustomCaptureActivity activity);

    void onClick(CustomCaptureActivity activity, View view);
  }

  public int getDecoratedBarcodeViewId() {
    return decoratedBarcodeViewId;
  }

  public ViewCallback getViewCallback() {
    return viewCallback;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public BarCodeInfoCallback getBarCodeInfoCallback() {
    return barCodeInfoCallback;
  }

  public void setBarCodeInfoCallback(BarCodeInfoCallback barCodeInfoCallback) {
    this.barCodeInfoCallback = barCodeInfoCallback;
  }

  public int getResultProcessing() {
    return resultProcessing;
  }

  public void setResultProcessing(int resultProcessing) {
    this.resultProcessing = resultProcessing;
  }

  public int getScanResult() {
    return scanResult;
  }

  public void setScanResult(int scanResult) {
    this.scanResult = scanResult;
  }
}
