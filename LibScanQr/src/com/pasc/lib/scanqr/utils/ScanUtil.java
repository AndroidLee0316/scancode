package com.pasc.lib.scanqr.utils;

import android.os.Build;

public class ScanUtil {



    public static boolean isXiaomiMobilePhone() {
        return "Xiaomi".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean isVivoMobilePhone() {
        return "vivo".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean isHuaweiMobilePhone() {
        return "huawei".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean isMeizuMobilePhone() {
        return "meizu".equalsIgnoreCase(Build.MANUFACTURER);
    }

}
