package com.pasc.lib.scanqr;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;

import com.pasc.lib.barcodescanner.DecoratedBarcodeView;
import com.pasc.lib.scanqr.utils.NavigationBarUtil;

public class CustomCaptureActivity extends AppCompatActivity implements View.OnClickListener {

    private CustomCaptureManager mCaptureManager;

    private Bundle savedInstanceState;
    private ScanQrManager.ViewCallback mViewCallback;
    private DecoratedBarcodeView mDecoratedBarcodeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        // 设置竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        @LayoutRes int decoratedBarcodeViewId = ScanQrManager.getInstance().getDecoratedBarcodeViewId();
        if (decoratedBarcodeViewId != 0){
            try{
                setContentView(decoratedBarcodeViewId);
            } catch (Exception e){
                e.printStackTrace();
                //id 不对
                return;
            }
        }
        if (NavigationBarUtil.checkDeviceHasNavigationBar(this)) {
            NavigationBarUtil.assistActivity(findViewById(android.R.id.content));
        }
        mViewCallback = ScanQrManager.getInstance().getViewCallback();
        if (mViewCallback != null){
            mViewCallback.initView(this);
        }
        checkPermission();

    }

    private void requestPermissions() {
        mDecoratedBarcodeView = null;
        try{
            mDecoratedBarcodeView = (DecoratedBarcodeView) findViewById(R.id.zxing_detector_barcode_view);
        } catch (Exception e){
            e.printStackTrace();
            //view ID is incorrect；
            return;
        }
        mCaptureManager =
                new CustomCaptureManager(CustomCaptureActivity.this,
                        mDecoratedBarcodeView);
        mCaptureManager.initializeFromIntent(getIntent(), savedInstanceState);
        mCaptureManager.decode();
    }

    private void checkPermission() {
        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != mCaptureManager) {
            mCaptureManager.onResume();
            mCaptureManager.resScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mCaptureManager) {
            mCaptureManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mCaptureManager) {
            mCaptureManager.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCaptureManager) {
            mCaptureManager.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mDecoratedBarcodeView == null){
            return super.onKeyDown(keyCode, event);
        }
        return mDecoratedBarcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        if (mViewCallback != null){
            mViewCallback.onClick(this, view);
        }
    }


}
