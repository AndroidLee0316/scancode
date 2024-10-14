//package com.pasc.lib.net;
//
//import android.support.annotation.NonNull;
//import android.text.TextUtils;
//import com.pasc.lib.net.param.BaseParam;
//import com.pasc.lib.net.resp.BaseResp;
//import com.pasc.lib.scanqr.ScanQrManager;
//import retrofit2.Call;
//
///**
// * Created by lanshaomin
// * Date: 2019/7/22 下午5:11
// * Desc:网络管理
// */
//public class NetManager {
//  /**
//   *
//   *
//   */
//  protected static QrcodeResp getWeatherDetailsInfoFromNet(String codeId) {
//    if (TextUtils.isEmpty(codeId)) {
//      return null;
//    }
//    QrcodeParam params = new QrcodeParam(codeId);
//
//    if (params == null) {
//      throw new IllegalArgumentException("params is null");
//    }
//
//    //try {
//    //  Call<BaseResp<QrcodeResp>> respCall;
//    //    respCall = ApiGenerator.createApi(ScanQrManager.getInstance().getUrl(), QrCodeApi.class));
//    //  BaseResp<QrcodeResp> baseResp = respCall.execute().body();
//    //  if (baseResp == null) {
//    //    return null;
//    //  }
//      return baseResp.data;
//    } catch (Exception e) {
//    }
//    return null;
//    return null;
//  }
//
//}
