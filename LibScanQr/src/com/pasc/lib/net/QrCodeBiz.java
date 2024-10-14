package com.pasc.lib.net;

import android.text.TextUtils;
import com.pasc.lib.net.param.BaseParam;
import com.pasc.lib.net.transform.RespTransformer;
import com.pasc.lib.scanqr.ScanQrManager;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by lanshaomin
 * Date: 2019/7/22 下午4:50
 * Desc:app上二维码详情展示接口请求
 */
public class QrCodeBiz {
  public static final String qrCodeQueryDetail="api/app/qrCode/checkQrCode";

  public static Single<QrcodeResp> checkQrCode(String codeContent){
    String url=TextUtils.isEmpty(ScanQrManager.getInstance().getUrl())?qrCodeQueryDetail:ScanQrManager.getInstance().getUrl();
    RespTransformer<QrcodeResp> respTransformer = RespTransformer.newInstance();
    return ApiGenerator.createApi(QrCodeApi.class)
        .checkResult(new QrcodeParam(codeContent),url)
        .compose(respTransformer)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }
}
