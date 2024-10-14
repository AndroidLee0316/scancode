package com.pasc.lib.scanqr;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import com.pasc.lib.barcodescanner.BarcodeResult;
import com.pasc.lib.barcodescanner.CameraPreview;
import com.pasc.lib.barcodescanner.CaptureManager;
import com.pasc.lib.barcodescanner.DecoratedBarcodeView;
import com.pasc.lib.barcodescanner.BarcodeCallback;
import com.pasc.lib.barcodescanner.camera.CameraBrightnessCallback;
import com.pasc.lib.scanqr.utils.ScanUtil;
import com.pasc.lib.zxing.ResultMetadataType;
import com.pasc.lib.zxing.ResultPoint;
import com.pasc.lib.zxing.client.android.BeepManager;
import com.pasc.lib.zxing.client.android.InactivityTimer;
import com.pasc.lib.zxing.client.android.Intents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class CustomCaptureManager {
  private static final String TAG = CaptureManager.class.getSimpleName();

  public static int cameraPermissionReqCode = 250;

  private Activity activity;
  private DecoratedBarcodeView barcodeView;
  private int orientationLock = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
  private static final String SAVED_ORIENTATION_LOCK = "SAVED_ORIENTATION_LOCK";
  private boolean returnBarcodeImagePath = false;

  private static final long DELAY_BEEP = 10;

  private InactivityTimer inactivityTimer;
  private BeepManager beepManager;

  private Handler handler;
  private CameraBrightnessCallback cameraBrightnessCallback;

  private BarcodeCallback callback = new BarcodeCallback() {
    @Override
    public void barcodeResult(final BarcodeResult result) {

      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          returnResult(result);
        }
      }, DELAY_BEEP);
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {

    }
  };

  private final CameraPreview.StateListener stateListener = new CameraPreview.StateListener() {
    @Override
    public void previewSized() {

    }

    @Override
    public void previewStarted() {

    }

    @Override
    public void previewStopped() {

    }

    @Override
    public void cameraError(Exception error) {
      displayFrameworkBugMessageAndExit();
    }
  };

  public CustomCaptureManager(Activity activity, DecoratedBarcodeView barcodeView) {
    this.activity = activity;
    this.barcodeView = barcodeView;
    barcodeView.getBarcodeView().addStateListener(stateListener);

    handler = new Handler();

    inactivityTimer = new InactivityTimer(activity, new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Finishing due to inactivity");
        finish();
      }
    });

    beepManager = new BeepManager(activity);
  }

  public BarcodeCallback getCallback() {
    return callback;
  }

  public void setCallback(BarcodeCallback callback) {
    this.callback = callback;
  }

  public void setCameraBrightnessCallback(
      CameraBrightnessCallback cameraBrightnessCallback) {
    this.cameraBrightnessCallback = cameraBrightnessCallback;
    if (barcodeView.getBarcodeView() != null) {
      barcodeView.getBarcodeView().setCameraBrightnessCallback(cameraBrightnessCallback);
    }
  }

  public void initializeFromIntent(Intent intent, Bundle savedInstanceState) {
    Window window = activity.getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    if (savedInstanceState != null) {
      // If the screen was locked and unlocked again, we may start in a different orientation
      // (even one not allowed by the manifest). In this case we restore the orientation we were
      // previously locked to.
      this.orientationLock = savedInstanceState.getInt(SAVED_ORIENTATION_LOCK,
          ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    if (intent != null) {
      if (orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
        // Only lock the orientation if it's not locked to something else yet
        boolean orientationLocked =
            intent.getBooleanExtra(Intents.Scan.ORIENTATION_LOCKED, true);

        if (orientationLocked) {
          lockOrientation();
        }
      }

      if (Intents.Scan.ACTION.equals(intent.getAction())) {
        barcodeView.initializeFromIntent(intent);
      }

      if (!intent.getBooleanExtra(Intents.Scan.BEEP_ENABLED, true)) {
        beepManager.setBeepEnabled(false);
        beepManager.updatePrefs();
      }

      if (intent.hasExtra(Intents.Scan.TIMEOUT)) {
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            returnResultTimeout();
          }
        };
        handler.postDelayed(runnable, intent.getLongExtra(Intents.Scan.TIMEOUT, 0L));
      }

      if (intent.getBooleanExtra(Intents.Scan.BARCODE_IMAGE_ENABLED, false)) {
        returnBarcodeImagePath = true;
      }
    }
  }

  protected void lockOrientation() {
    // Only get the orientation if it's not locked to one yet.
    if (this.orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
      // Adapted from http://stackoverflow.com/a/14565436
      Display display = activity.getWindowManager().getDefaultDisplay();
      int rotation = display.getRotation();
      int baseOrientation = activity.getResources().getConfiguration().orientation;
      int orientation = 0;
      if (baseOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else {
          orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        }
      } else if (baseOrientation == Configuration.ORIENTATION_PORTRAIT) {
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
          orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
      }

      this.orientationLock = orientation;
    }
    //noinspection ResourceType
    activity.setRequestedOrientation(this.orientationLock);
  }

  public void decode() {
    barcodeView.decodeSingle(callback);
  }

  public void resScan() {
    barcodeView.decodeSingle(callback);
  }

  public void onResume() {
    if (Build.VERSION.SDK_INT >= 23) {
      openCameraWithPermission();
    } else {
      barcodeView.resume();
    }
    beepManager.updatePrefs();
    inactivityTimer.start();
  }

  private boolean askedPermission = false;

  @TargetApi(23)
  private void openCameraWithPermission() {
    if (!ScanUtil.isVivoMobilePhone()) {
      if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.CAMERA)
          == PackageManager.PERMISSION_GRANTED) {

        barcodeView.resume();
      } else {
        if (!askedPermission) {
          ActivityCompat.requestPermissions(this.activity,
              new String[] { Manifest.permission.CAMERA },
              cameraPermissionReqCode);
          askedPermission = true;
        }
      }
    } else {
      onRequestPermissions_vivo();
    }
  }

  private boolean isHasPermission() {
    Field fieldPassword = null;
    try {
      Camera camera = Camera.open();
      fieldPassword = camera.getClass().getDeclaredField("mHasPermission");
      fieldPassword.setAccessible(true);
      return (boolean) fieldPassword.get(camera);
    } catch (Exception e) {
      e.printStackTrace();
      return true;
    }
  }

  public void onRequestPermissionsResult(int requestCode, String permissions[],
      int[] grantResults) {
    if (requestCode == cameraPermissionReqCode) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        barcodeView.resume();
      }
    }
  }

  public void onRequestPermissions_vivo() {
    if (isHasPermission()) {

      barcodeView.resume();
    } else {
      // TODO 申请权限

    }
  }

  public void onPause() {
    barcodeView.pause();

    inactivityTimer.cancel();
    beepManager.close();
  }

  public void onDestroy() {
    inactivityTimer.cancel();
  }

  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(SAVED_ORIENTATION_LOCK, this.orientationLock);
  }

  public static Intent resultIntent(BarcodeResult rawResult, String barcodeImagePath) {
    Intent intent = new Intent(Intents.Scan.ACTION);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
    intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
    byte[] rawBytes = rawResult.getRawBytes();
    if (rawBytes != null && rawBytes.length > 0) {
      intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
    }
    Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
    if (metadata != null) {
      if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
        intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
            metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString());
      }
      Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
      if (orientation != null) {
        intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
      }
      String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
      if (ecLevel != null) {
        intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
      }
      @SuppressWarnings("unchecked") Iterable<byte[]> byteSegments =
          (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
      if (byteSegments != null) {
        int i = 0;
        for (byte[] byteSegment : byteSegments) {
          intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
          i++;
        }
      }
    }
    if (barcodeImagePath != null) {
      intent.putExtra(Intents.Scan.RESULT_BARCODE_IMAGE_PATH, barcodeImagePath);
    }
    return intent;
  }

  private String getBarcodeImagePath(BarcodeResult rawResult) {
    String barcodeImagePath = null;
    if (returnBarcodeImagePath) {
      Bitmap bmp = rawResult.getBitmap();
      try {
        File bitmapFile =
            File.createTempFile("barcodeimage", ".jpg", activity.getCacheDir());
        FileOutputStream outputStream = new FileOutputStream(bitmapFile);
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        outputStream.close();
        barcodeImagePath = bitmapFile.getAbsolutePath();
      } catch (IOException e) {
        Log.w(TAG, "Unable to create temporary file and store bitmap! " + e);
      }
    }
    return barcodeImagePath;
  }

  private void finish() {
    activity.finish();
  }

  protected void returnResultTimeout() {
    Intent intent = new Intent(Intents.Scan.ACTION);
    intent.putExtra(Intents.Scan.TIMEOUT, true);
    activity.setResult(Activity.RESULT_CANCELED, intent);
    finish();
  }

  protected void returnResult(BarcodeResult rawResult) {
    Intent intent = resultIntent(rawResult, getBarcodeImagePath(rawResult));
    activity.setResult(Activity.RESULT_OK, intent);
    finish();
  }

  protected void displayFrameworkBugMessageAndExit() {

  }

  protected void  playBeepSoundAndVibrate(){
    beepManager.playBeepSoundAndVibrate();
  }
}
