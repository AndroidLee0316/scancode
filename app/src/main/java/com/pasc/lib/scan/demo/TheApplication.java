package com.pasc.lib.scan.demo;

import android.app.Application;
import android.content.Context;
import android.widget.ImageView;
import com.pasc.lib.barcodescanner.BarCodeInfoCallback;
import com.pasc.lib.base.AppProxy;
import com.pasc.lib.base.util.AppUtils;
import com.pasc.lib.base.util.ToastUtils;
import com.pasc.lib.hybrid.HybridInitConfig;
import com.pasc.lib.hybrid.PascHybrid;
import com.pasc.lib.hybrid.callback.HybridInitCallback;
import com.pasc.lib.net.NetConfig;
import com.pasc.lib.net.NetManager;
import com.pasc.lib.net.download.DownLoadManager;
import com.pasc.lib.scanqr.ScanQrManager;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

public class TheApplication extends Application {
  /**
   * 基线测试环境
   */
  private static final String HOST_URL = "http://basesmt-caas.yun.city.pingan.com/";

  @Override
  public void onCreate() {
    super.onCreate();
    //主进程
    if (AppUtils.getPIDName(this).equals(getPackageName())) {
      AppProxy.getInstance().init(this, false)
          .setIsDebug(BuildConfig.DEBUG)
          .setProductType(1)
          .setHost(HOST_URL)
          .setVersionName(BuildConfig.VERSION_NAME);
      initNet();
      initHybrid();
      initScanQr();
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
  }

  /****初始化网络****/
  private void initNet() {
    NetConfig config = new NetConfig.Builder(this)
        .baseUrl(HOST_URL)
        .isDebug(BuildConfig.DEBUG)
        .build();
    NetManager.init(config);

    DownLoadManager.getDownInstance().init(this, 3, 5, 0);
  }

  /**
   * 初始化hybrid
   */
  private void initHybrid() {

    PascHybrid.getInstance().init(new HybridInitConfig()
        .setHybridInitCallback(new HybridInitCallback() {
          @Override
          public void loadImage(ImageView imageView, String url) {
          }

          @Override
          public void setWebSettings(WebSettings settings) {
            settings.setUserAgent(settings.getUserAgentString()
                + "/openweb=paschybrid/MaanshanSMT_Android,VERSION:"
                + BuildConfig.VERSION_NAME);
          }

          @Override
          public String themeColorString() {
            return "#333333";
          }

          @Override public void onWebViewCreate(WebView webView) {

          }

          @Override public void onWebViewProgressChanged(WebView webView, int i) {

          }

          @Override public void onWebViewPageFinished(WebView webView, String s) {

          }
        }));
  }

  private void initScanQr() {
    //url地址
    ScanQrManager.getInstance().setUrl("api/app/qrCode/checkQrCode");
    //扫码结果为h5还是原生处理，默认H5
    ScanQrManager.getInstance().setScanResult(ScanQrManager.SCAN_RESULT_H5);
    //结果是在扫码页还是上一页面处理 默认扫码页
    ScanQrManager.getInstance().setResultProcessing(ScanQrManager.SCAN_RESULT_CURRENT_PAGE);
    //在扫码页处理原生结果的回调
    ScanQrManager.getInstance().setBarCodeInfoCallback(new BarCodeInfoCallback() {
      @Override public void getBarCodeResult(String s) {
      }
    });
  }
}

