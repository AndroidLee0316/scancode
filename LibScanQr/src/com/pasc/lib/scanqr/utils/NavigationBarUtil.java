package com.pasc.lib.scanqr.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.reflect.Method;

/**
 * Created by zhangxu678 on 2018/9/25.
 */
public class NavigationBarUtil {

  public static void assistActivity(View content) {
    new NavigationBarUtil(content);
  }

  private View mChildOfContent;
  private int usableHeightPrevious;
  private ViewGroup.LayoutParams frameLayoutParams;

  private NavigationBarUtil(View content) {
    mChildOfContent = content;
    mChildOfContent.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          public void onGlobalLayout() {
            possiblyResizeChildOfContent();
          }
        });
    frameLayoutParams = mChildOfContent.getLayoutParams();
  }

  private void possiblyResizeChildOfContent() {
    int usableHeightNow = computeUsableHeight();
    if (usableHeightNow != usableHeightPrevious) {

      frameLayoutParams.height = usableHeightNow;
      mChildOfContent.requestLayout();
      usableHeightPrevious = usableHeightNow;
    }
  }

  private int computeUsableHeight() {
    Rect r = new Rect();
    mChildOfContent.getWindowVisibleDisplayFrame(r);
    return (r.bottom);
  }

  public static boolean checkDeviceHasNavigationBar(Context context) {
    boolean hasNavigationBar = false;
    Resources rs = context.getResources();
    int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
    if (id > 0) {
      hasNavigationBar = rs.getBoolean(id);
    }
    try {
      Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
      Method m = systemPropertiesClass.getMethod("get", String.class);
      String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
      if ("1".equals(navBarOverride)) {
        hasNavigationBar = false;
      } else if ("0".equals(navBarOverride)) {
        hasNavigationBar = true;
      }
    } catch (Exception e) {

    }
    return hasNavigationBar;
  }
}


