package com.pasc.lib.net;

import com.pasc.lib.net.param.BaseParam;
import com.pasc.lib.net.resp.BaseResp;
import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by lanshaomin
 * Date: 2019/7/22 下午4:50
 * Desc:
 */
interface QrCodeApi {
  /**
   *  判断二维码内容
   */
  @POST Single<BaseResp<QrcodeResp>> checkResult(
      @Body QrcodeParam param, @Url String url);
}
