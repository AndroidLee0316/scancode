package com.pasc.lib.scanqr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.pasc.lib.barcodescanner.BarCodeInfoCallback;
import com.pasc.lib.barcodescanner.BarcodeCallback;
import com.pasc.lib.barcodescanner.BarcodeResult;
import com.pasc.lib.barcodescanner.DecoratedBarcodeView;
import com.pasc.lib.barcodescanner.camera.CameraBrightnessCallback;
import com.pasc.lib.base.permission.PermissionUtils;
import com.pasc.lib.hybrid.PascHybrid;
import com.pasc.lib.hybrid.nativeability.WebStrategy;
import com.pasc.lib.net.QrCodeBiz;
import com.pasc.lib.net.QrcodeResp;
import com.pasc.lib.picture.pictureSelect.NewPictureSelectActivity;
import com.pasc.lib.picture.takephoto.app.TakePhoto;
import com.pasc.lib.picture.takephoto.app.TakePhotoImpl;
import com.pasc.lib.picture.takephoto.compress.CompressConfig;
import com.pasc.lib.picture.takephoto.model.InvokeParam;
import com.pasc.lib.picture.takephoto.model.TContextWrap;
import com.pasc.lib.picture.takephoto.model.TImage;
import com.pasc.lib.picture.takephoto.model.TResult;
import com.pasc.lib.picture.takephoto.model.TakePhotoOptions;
import com.pasc.lib.picture.takephoto.permission.InvokeListener;
import com.pasc.lib.picture.takephoto.permission.PermissionManager;
import com.pasc.lib.picture.takephoto.permission.TakePhotoInvocationHandler;
import com.pasc.lib.router.RouterTable;
import com.pasc.lib.scanqr.utils.BarUtils;
import com.pasc.lib.scanqr.utils.ImageDecodeUtil;
import com.pasc.lib.statistics.StatisticsManager;
import com.pasc.lib.widget.NetworkUtils;
import com.pasc.lib.widget.dialog.DialogFragmentInterface;
import com.pasc.lib.widget.dialog.OnCloseListener;
import com.pasc.lib.widget.dialog.OnConfirmListener;
import com.pasc.lib.widget.dialog.common.AnimationType;
import com.pasc.lib.widget.dialog.common.ButtonWrapper;
import com.pasc.lib.widget.dialog.common.ConfirmDialogFragment;
import com.pasc.lib.widget.dialog.common.PermissionDialogFragment2;
import com.pasc.lib.widget.dialog.loading.LoadingDialogFragment;
import com.pasc.lib.widget.toast.Toasty;
import com.pasc.lib.zxing.Result;
import com.pasc.lib.zxing.ResultPoint;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Route(path = RouterTable.SCAN_CODE)
public class CustomStandardCaptureActivity extends AppCompatActivity
    implements View.OnClickListener, BarcodeCallback, CameraBrightnessCallback,
    TakePhoto.TakeResultListener, InvokeListener {
  private static final String TAG = "CustomStandardCaptureAc";

  private static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 0x1010;
  private static final int REQUEST_PICK_IMAGE = 12;
  private static final int REQUEST_ALBUM_OK = 0x1230;
  private CustomCaptureManager mCaptureManager;
  private DecoratedBarcodeView mDecoratedBarcodeView;
  private CustomViewfinderView mFinderView;
  private String mBarcode;
  private boolean mIsOpenFlashlight;
  private ImageView mFlashlightIb;
  private ImageView imgBack;
  private TextView tvFlashlight;
  private Bundle savedInstanceState;
  private TextView gallery;
  private TextView detectorTips;
  private View mFlashContainer;
  private ExecutorService executorService = Executors.newSingleThreadExecutor();
  private TakePhoto takePhoto;
  private InvokeParam invokeParam;
  private CompositeDisposable mDisposables = new CompositeDisposable();
  private BarCodeInfoCallback mBarCodeInfoCallback;
  private LoadingDialogFragment mLoadingDialog;
  /**
   * 统计ID
   */
  private String eventId;
  /**
   * 统计标签
   */
  private String eventLabel;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    this.savedInstanceState = savedInstanceState;
    // 设置竖屏
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    setContentView(R.layout.pasc_zxing_custom_standard_scan_activity);

    BarUtils.setStatusBarVisibility(this, true);
    BarUtils.transparentStatusBar(this);
    NewPictureSelectActivity.setIsHeadImg(true);
    getTakePhoto().customPickActivity(NewPictureSelectActivity.class);
    //路由跳转传递的参数
    Intent intent = getIntent();
    if (intent != null) {
      eventId = intent.getStringExtra("eventId");
      eventLabel = intent.getStringExtra("eventLabel");
      if (intent.hasExtra("resultProcessing")) {
        ScanQrManager.getInstance()
            .setResultProcessing(
                intent.getIntExtra("resultProcessing", ScanQrManager.SCAN_RESULT_CURRENT_PAGE));
      }
      if (intent.hasExtra("scanResult")) {
        ScanQrManager.getInstance()
            .setScanResult(intent.getIntExtra("scanResult", ScanQrManager.SCAN_RESULT_H5));
      }
    }

    initView();
    checkPermission();
  }

  @Override protected void onStart() {
    super.onStart();
    //checkPermission();
  }

  private void initView() {

    mFinderView = (CustomViewfinderView) findViewById(R.id.zxing_viewfinder_view);
    imgBack = (ImageView) findViewById(R.id.zxing_standard_top_back);
    gallery = (TextView) findViewById(R.id.zxing_standard_top_gallery);
    detectorTips = findViewById(R.id.food_tv_food_detector_tips);

    tvFlashlight = (TextView) findViewById(R.id.food_tv_food_detect_flashlight_tips);

    imgBack.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    mFlashlightIb = (ImageView) findViewById(R.id.food_ibt_food_detector_flashlight);
    mFlashContainer = findViewById(R.id.qrcode_fashlight_container);
    mFlashlightIb.setOnClickListener(this);
    gallery.setOnClickListener(this);
    mBarCodeInfoCallback = ScanQrManager.getInstance().getBarCodeInfoCallback();
  }

  private void requestPermissions() {
    mDecoratedBarcodeView = initializeContent();
    mCaptureManager =
        new CustomCaptureManager(CustomStandardCaptureActivity.this, mDecoratedBarcodeView);
    mCaptureManager.initializeFromIntent(getIntent(), savedInstanceState);
    mCaptureManager.setCallback(this);
    mCaptureManager.decode();
    mCaptureManager.setCameraBrightnessCallback(this);
  }

  private void checkPermission() {
    //requestPermissions();

    PermissionUtils.request(this, Manifest.permission.CAMERA).subscribe(new Consumer<Boolean>() {
      @Override
      public void accept(Boolean aBoolean) throws Exception {
        if (aBoolean) {
          requestPermissions();
          showNoPermission();
        } else {
          //开启相机
          ButtonWrapper buttonWrapper =
              ButtonWrapper.wapButton("去开启", R.color.white, R.drawable.selector_primary_button);
          showPermissionDialog("开启相机", "为您提供更完善的服务", R.mipmap.ic_permisson_camera, buttonWrapper,
              true);
          hideNoPermission();
        }
      }
    });
    //if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    //    != PackageManager.PERMISSION_GRANTED) {
    //  showNoPermission();
    //} else {
    //  hideNoPermission();
    //}
  }

  /**
   * 识别二维码
   */
  private void checkQrCode(String codeId) {
    showDialog();
    mDisposables.add(QrCodeBiz.checkQrCode(codeId)
        .subscribe(new Consumer<QrcodeResp>() {
          @Override
          public void accept(QrcodeResp qrcodeResp) throws Exception {
            if (ScanQrManager.getInstance().getResultProcessing()
                == ScanQrManager.SCAN_RESULT_CURRENT_PAGE) {
              PascHybrid.getInstance()
                  .start(CustomStandardCaptureActivity.this,
                      new WebStrategy().setUrl(qrcodeResp.codePath));
              if (!TextUtils.isEmpty(eventId) && !TextUtils.isEmpty(eventLabel)) {
                HashMap<String, String> map = new HashMap<>();
                map.put("页面名称", qrcodeResp.codePath);
                StatisticsManager.getInstance().onEvent("个人版首页-扫码-扫码使用", "跳转页面", map);
              }
            } else {
              Intent intent = new Intent();
              intent.putExtra("SCAN_RESULT", qrcodeResp.codePath);
              setResult(RESULT_OK, intent);
            }
            finish();
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            mLoadingDialog.dismiss();
            if (!NetworkUtils.isNetworkAvailable(CustomStandardCaptureActivity.this)) {
              Toasty.init(getApplicationContext())
                  .setMessage(getResources().getString(R.string.weather_network_unavailable))
                  .show();
            }
          }
        }));
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
    mDisposables.dispose();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[],
      int[] grantResults) {
    PermissionManager.TPermissionType type =
        PermissionManager.onRequestPermissionsResult(requestCode, permissions,
            grantResults);
    PermissionManager.handlePermissionsResult(this, type, invokeParam, this);
    if (requestCode == REQUEST_PERMISSION_EXTERNAL_STORAGE &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      //已经授权;
      //后面接口调用ok;
      selectImageFromGallery();
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (mDecoratedBarcodeView == null) {
      return super.onKeyDown(keyCode, event);
    }
    return mDecoratedBarcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
  }

  public void showNoPermission() {
    //mFlashlightIb.setVisibility(View.GONE);
    //tvFlashlight.setVisibility(View.GONE);
    mFlashContainer.setVisibility(View.INVISIBLE);
  }

  public void hideNoPermission() {
    //mFlashlightIb.setVisibility(View.VISIBLE);
    //tvFlashlight.setVisibility(View.VISIBLE);
    mFlashContainer.setVisibility(View.VISIBLE);
  }

  public void handleBarCodetoServer(String barcode) {
    if (mCaptureManager == null) {
      return;
    }
  }

  /**
   * Override to use a different layout.
   *
   * @return the DecoratedBarcodeView
   */
  protected DecoratedBarcodeView initializeContent() {

    DecoratedBarcodeView view =
        (DecoratedBarcodeView) findViewById(R.id.zxing_detector_barcode_view);
    return view;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    getTakePhoto().onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_PICK_IMAGE:
        if (data != null) {
          Uri uri = data.getData();
          if (uri != null) {
            Log.d("uri", uri.toString());
          }
        }
        break;
      case REQUEST_ALBUM_OK:
        if (null != data.getData()) {
          Log.d(TAG, "onActivityResult:相册 " + data.getData().toString());
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void onClick(View view) {
    int i = view.getId();
    if (i == R.id.food_ibt_food_detector_flashlight) {
      clickFlashlight();
    } else if (i == R.id.zxing_standard_top_gallery) {
      selectImageFromGallery();
    }
  }

  private void selectImageFromGallery() {
    //先获取读取sd卡的权限
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
      //申请获取imei权限
      ActivityCompat.requestPermissions(this,
          new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
          REQUEST_PERMISSION_EXTERNAL_STORAGE);
      return;
    }
    //Intent albumIntent = new Intent(Intent.ACTION_PICK, null);
    //albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
    //startActivityForResult(albumIntent, REQUEST_ALBUM_OK);
    configTakePhotoOption(getTakePhoto());
    configCompress(getTakePhoto());
    getTakePhoto().onPickMultiple(1);
  }

  private void clickFlashlight() {
    mIsOpenFlashlight = !mIsOpenFlashlight;

    if (null != mFlashlightIb) {
      if (mIsOpenFlashlight) {
        //开启闪光灯
        mFlashlightIb.setImageResource(R.mipmap.zxing_flashlight_on);
        if (null != mDecoratedBarcodeView) {
          mDecoratedBarcodeView.setTorchOn();
        }
      } else {
        //关闭闪光灯
        mFlashlightIb.setImageResource(R.mipmap.zxing_flashlight_off);
        if (null != mDecoratedBarcodeView) {
          mDecoratedBarcodeView.setTorchOff();
        }
      }
    }
  }

  @Override public void barcodeResult(BarcodeResult result) {
    mCaptureManager.playBeepSoundAndVibrate();
    if (ScanQrManager.getInstance().getScanResult() == ScanQrManager.SCAN_RESULT_H5) {
      checkQrCode(result.getText());
    } else {
      if (ScanQrManager.getInstance().getResultProcessing()
          == ScanQrManager.SCAN_RESULT_CURRENT_PAGE) {
        if (mBarCodeInfoCallback != null) {
          mBarCodeInfoCallback.getBarCodeResult(result.getText());
        }
      } else {
        Intent intent = new Intent();
        intent.putExtra("SCAN_RESULT", result.getText());
        setResult(RESULT_OK, intent);
      }
      finish();
    }
  }

  @Override public void possibleResultPoints(List<ResultPoint> resultPoints) {

  }

  @Override
  public void onCameraAmbientBrightnessChanged(final boolean isDark) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        if (!mIsOpenFlashlight) {
          if (isDark) {
            mFlashContainer.setVisibility(View.VISIBLE);
          } else {
            mFlashContainer.setVisibility(View.INVISIBLE);
          }
        }
      }
    });
  }

  /**
   * 获取TakePhoto实例
   */
  public TakePhoto getTakePhoto() {
    if (takePhoto == null) {
      takePhoto = (TakePhoto) TakePhotoInvocationHandler.of(this)
          .bind(new TakePhotoImpl(this, this));
    }
    return takePhoto;
  }

  /**
   * 配置图片属性
   */
  private void configTakePhotoOption(TakePhoto photo) {
    TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();
    //使用自带相册
    builder.setWithOwnGallery(false);
    //纠正旋转角度
    builder.setCorrectImage(false);
    photo.setTakePhotoOptions(builder.create());
  }

  /**
   * 配置压缩
   */
  private void configCompress(TakePhoto takePhoto) {
    //大小不超过100k
    CompressConfig config = new CompressConfig.Builder().setMaxSize(102400)
        //最大像素800
        .setMaxPixel(800)
        //是否压缩
        .enableReserveRaw(true)
        .create();
    //这个trued代表显示压缩进度条
    takePhoto.onEnableCompress(config, false);
  }

  @Override public void takeSuccess(TResult result) {
    Log.i(TAG, "takeSuccess：" + result.getImage().getCompressPath());
    ArrayList<TImage> images = result.getImages();
    final String path = images.get(images.size() - 1).getCompressPath();
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        final Result decode = ImageDecodeUtil.decode(CustomStandardCaptureActivity.this, path);
        if (decode != null) {
          if (ScanQrManager.getInstance().getScanResult() == ScanQrManager.SCAN_RESULT_H5) {
            runOnUiThread(new Runnable() {
              @Override public void run() {
                checkQrCode(decode.getText());
              }
            });
          } else {
            runOnUiThread(new Runnable() {
              @Override public void run() {
                if (ScanQrManager.getInstance().getResultProcessing()
                    == ScanQrManager.SCAN_RESULT_CURRENT_PAGE) {
                  if (mBarCodeInfoCallback != null) {
                    mBarCodeInfoCallback.getBarCodeResult(decode.getText());
                  }
                  finish();
                } else {
                  Intent intent = new Intent();
                  intent.putExtra("SCAN_RESULT", decode.getText());
                  setResult(RESULT_OK, intent);
                  finish();
                }
              }
            });
          }
        } else {
          showNoCodeDialog();
          Log.d(TAG, "run: decode -> null ");
        }
      }
    });
  }

  @Override public void takeFail(TResult tResult, String s) {

  }

  @Override public void takeCancel() {

  }

  @Override public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
    PermissionManager.TPermissionType
        type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod());
    if (PermissionManager.TPermissionType.WAIT.equals(type)) {
      this.invokeParam = invokeParam;
    }
    return type;
  }

  /**
   * 未发现码
   */
  private void showNoCodeDialog() {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        ConfirmDialogFragment confirmDialogFragment = new ConfirmDialogFragment.Builder()
            .setDesc("未发现二维码/条形码，请重新扫描")
            .setAnimationType(AnimationType.TRANSLATE_BOTTOM)
            .setConfirmText("我知道了")
            .setHideCloseButton(true)
            .setOnConfirmListener(new OnConfirmListener<ConfirmDialogFragment>() {
              @Override
              public void onConfirm(ConfirmDialogFragment dialogFragment) {
                dialogFragment.dismiss();
              }
            })
            .setOnCloseListener(new OnCloseListener<ConfirmDialogFragment>() {
              @Override
              public void onClose(ConfirmDialogFragment dialogFragment) {
                dialogFragment.dismiss();
              }
            })
            .build();
        confirmDialogFragment.show(CustomStandardCaptureActivity.this,
            "ConfirmDialogFragment");
      }
    });
  }

  /**
   * 权限开启型弹窗
   *
   * @param title 弹窗标题
   * @param desc 弹窗描述
   * @param iconResId 弹窗icon
   * @param buttonWrapper 弹窗button包装类
   */
  private void showPermissionDialog(String title, String desc, @DrawableRes int iconResId,
      final ButtonWrapper buttonWrapper, boolean closeImgVisible) {
    final PermissionDialogFragment2 permissionDialogFragment =
        new PermissionDialogFragment2.Builder()
            .setTitle(title)
            .setCloseImgVisible(closeImgVisible)
            .setDesc(desc)
            .setIconResId(iconResId)
            .setButton(buttonWrapper,
                new DialogFragmentInterface.OnClickListener<PermissionDialogFragment2>() {
                  @Override
                  public void onClick(PermissionDialogFragment2 dialogFragment, int which) {
                    dialogFragment.dismiss();
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                  }
                })
            .setOnCancelListener(
                new DialogFragmentInterface.OnCancelListener<PermissionDialogFragment2>() {
                  @Override
                  public void onCancel(PermissionDialogFragment2 dialogFragment) {
                    dialogFragment.dismiss();
                  }
                })
            .build();
    permissionDialogFragment.show(CustomStandardCaptureActivity.this, "ConfirmDialogFragment");
  }

  private void showDialog() {
    mLoadingDialog = new LoadingDialogFragment.Builder()
        .setCancelable(true)
        .build();
    mLoadingDialog.show(getSupportFragmentManager(), "LoadingDialogFragment");
  }
}
