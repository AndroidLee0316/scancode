package com.pasc.lib.scan.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.alibaba.android.arouter.launcher.ARouter;
import com.pasc.lib.barcodescanner.BarCodeInfoCallback;
import com.pasc.lib.net.QrcodeResp;
import com.pasc.lib.scanqr.ScanQrManager;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity__";

    private TextView resultTv;
    private static final int SCAN_QR_CODE = 24;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init(){
        findViewById(R.id.main_scan_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });
        resultTv = (TextView)findViewById(R.id.main_result_txt);
    }

    private void startScan(){
        //自定义扫码
//        ScanQrManager.getInstance().setContentView(com.pasc.lib.scanqr.R.layout.zxing_custom_standard_scan_activity);
//        ScanQrManager.getInstance().setViewCallback(new ScanQrManager.ViewCallback() {
//
//
//            public DecoratedBarcodeView mDecoratedBarcodeView;
//            public boolean mIsOpenFlashlight;
//            public ImageButton mFlashlightIb;
//            public TextView tvFlashlight;
//            public ImageView imgBack;
//            public CustomViewfinderView mFinderView;
//
//            @Override
//            public void initView (CustomCaptureActivity activity) {
//                mFinderView = (CustomViewfinderView) activity.findViewById(com.pasc.lib.scanqr.R.id.zxing_viewfinder_view);
//                imgBack = (ImageView) activity.findViewById(com.pasc.lib.scanqr.R.id.zxing_standard_top_back);
//                tvFlashlight = (TextView) activity.findViewById(com.pasc.lib.scanqr.R.id.food_tv_food_detect_flashlight_tips);
//                imgBack.setOnClickListener(activity);
//                mFlashlightIb = (ImageButton) activity.findViewById(com.pasc.lib.scanqr.R.id.food_ibt_food_detector_flashlight);
//                mFlashlightIb.setOnClickListener(activity);
//                mDecoratedBarcodeView = (DecoratedBarcodeView) activity.findViewById(com.pasc.lib.scanqr.R.id.zxing_detector_barcode_view);
//
//            }
//
//            @Override
//            public void onClick (CustomCaptureActivity activity, View view) {
//                int i = view.getId();
//                if (i == com.pasc.lib.scanqr.R.id.food_ibt_food_detector_flashlight) {
//                    clickFlashlight();
//                } else if (i == com.pasc.lib.scanqr.R.id.zxing_standard_top_back){
//                    activity.finish();
//                }
//            }
//
//            private void clickFlashlight() {
//                mIsOpenFlashlight = !mIsOpenFlashlight;
//
//                if (null != mFlashlightIb) {
//                    if (mIsOpenFlashlight) {
//                        //开启闪光灯
//                        mFlashlightIb.setImageResource(com.pasc.lib.scanqr.R.mipmap.zxing_flashlight_on);
//                        if (null != mDecoratedBarcodeView) {
//                            mDecoratedBarcodeView.setTorchOn();
//                        }
//
//                    } else {
//                        //关闭闪光灯
//                        mFlashlightIb.setImageResource(com.pasc.lib.scanqr.R.mipmap.zxing_flashlight_off);
//                        if (null != mDecoratedBarcodeView) {
//                            mDecoratedBarcodeView.setTorchOff();
//                        }
//                    }
//                }
//            }
//
//        });
//        ScanQrManager.getInstance().startScan(this);
        Bundle bundle=new Bundle();
        //bundle.putString("eventId","扫一扫");
        //bundle.putString("eventLabel","测试");
        bundle.putInt("resultProcessing",ScanQrManager.SCAN_RESULT_CURRENT_PAGE);
        bundle.putInt("scanResult",ScanQrManager.SCAN_RESULT_NATIVITE);
        ScanQrManager.getInstance().startScan(this);
//        Intent intent = new Intent(MainActivity.this, CustomStandardCaptureActivity.class);
//        startActivityForResult(intent, SCAN_QR_CODE);
        Log.e(TAG, "startScan: start " );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_QR_CODE){
            if (data != null){
                String result = data.getStringExtra("SCAN_RESULT");
                Log.e(TAG, "startScan: result -> "+result );

                if (!TextUtils.isEmpty(result)){
                    resultTv.setText(result);
                }
            }
        }
    }
}
