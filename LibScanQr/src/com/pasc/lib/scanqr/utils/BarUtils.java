package com.pasc.lib.scanqr.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public final class BarUtils {

  private static final String TAG_STATUS_BAR = "TAG_STATUS_BAR";
  private static final String TAG_OFFSET = "TAG_OFFSET";
  private static final int KEY_OFFSET = -123;

  public static void setStatusBarVisibility(@NonNull final Activity activity,
      final boolean isVisible) {
    setStatusBarVisibility(activity.getWindow(), isVisible);
  }

  public static void setStatusBarVisibility(@NonNull final Window window,
      final boolean isVisible) {
    if (isVisible) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      showStatusBarView(window);
      addMarginTopEqualStatusBarHeight(window);
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      hideStatusBarView(window);
      subtractMarginTopEqualStatusBarHeight(window);
    }
  }

  private static void showStatusBarView(final Window window) {
    ViewGroup decorView = (ViewGroup) window.getDecorView();
    View fakeStatusBarView = decorView.findViewWithTag(TAG_STATUS_BAR);
    if (fakeStatusBarView == null) return;
    fakeStatusBarView.setVisibility(View.VISIBLE);
  }

  private static void hideStatusBarView(final Window window) {
    ViewGroup decorView = (ViewGroup) window.getDecorView();
    View fakeStatusBarView = decorView.findViewWithTag(TAG_STATUS_BAR);
    if (fakeStatusBarView == null) return;
    fakeStatusBarView.setVisibility(View.GONE);
  }

  private static void addMarginTopEqualStatusBarHeight(final Window window) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
    View withTag = window.getDecorView().findViewWithTag(TAG_OFFSET);
    if (withTag == null) return;
    addMarginTopEqualStatusBarHeight(withTag);
  }

  public static void addMarginTopEqualStatusBarHeight(@NonNull View view) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
    view.setTag(TAG_OFFSET);
    Object haveSetOffset = view.getTag(KEY_OFFSET);
    if (haveSetOffset != null && (Boolean) haveSetOffset) return;
    ViewGroup.MarginLayoutParams layoutParams =
        (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    layoutParams.setMargins(layoutParams.leftMargin,
        layoutParams.topMargin + getStatusBarHeight(),
        layoutParams.rightMargin,
        layoutParams.bottomMargin);
    view.setTag(KEY_OFFSET, true);
  }

  public static int getStatusBarHeight() {
    Resources resources = Resources.getSystem();
    int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
    return resources.getDimensionPixelSize(resourceId);
  }

  private static void subtractMarginTopEqualStatusBarHeight(final Window window) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
    View withTag = window.getDecorView().findViewWithTag(TAG_OFFSET);
    if (withTag == null) return;
    subtractMarginTopEqualStatusBarHeight(withTag);
  }

  public static void subtractMarginTopEqualStatusBarHeight(@NonNull View view) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
    Object haveSetOffset = view.getTag(KEY_OFFSET);
    if (haveSetOffset == null || !(Boolean) haveSetOffset) return;
    ViewGroup.MarginLayoutParams layoutParams =
        (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    layoutParams.setMargins(layoutParams.leftMargin,
        layoutParams.topMargin - getStatusBarHeight(),
        layoutParams.rightMargin,
        layoutParams.bottomMargin);
    view.setTag(KEY_OFFSET, false);
  }

  public static void transparentStatusBar(final Activity activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
    Window window = activity.getWindow();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      int option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        int vis = window.getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        window.getDecorView().setSystemUiVisibility(option | vis);
      } else {
        window.getDecorView().setSystemUiVisibility(option);
      }
      window.setStatusBarColor(Color.TRANSPARENT);
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }
  }
}
