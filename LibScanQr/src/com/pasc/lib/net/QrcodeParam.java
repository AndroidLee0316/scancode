package com.pasc.lib.net;

import com.google.gson.annotations.SerializedName;

/**
 * Created by lanshaomin
 * Date: 2019/7/22 下午4:50
 * Desc:判断二维码内容
 */
public class QrcodeParam {
  @SerializedName("codeContent")
  public String codeContent;

  public QrcodeParam(String codeContent) {
    this.codeContent = codeContent;
  }
}
