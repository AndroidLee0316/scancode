package com.pasc.lib.barcodescanner;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import com.pasc.lib.barcodescanner.camera.CameraBrightnessCallback;
import com.pasc.lib.scanqr.R;
import com.pasc.lib.zxing.DecodeHintType;
import com.pasc.lib.zxing.ResultPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view for scanning barcodes.
 *
 * Two methods MUST be called to manage the state:
 * 1. resume() - initialize the camera and start the preview. Call from the Activity's onResume().
 * 2. pause() - stop the preview and release any resources. Call from the Activity's onPause().
 *
 * Start decoding with decodeSingle() or decodeContinuous(). Stop decoding with stopDecoding().
 *
 * @see CameraPreview for more details on the preview lifecycle.
 */
public class BarcodeView extends CameraPreview {

    private enum DecodeMode {
        NONE,
        SINGLE,
        CONTINUOUS
    }

    private DecodeMode decodeMode = DecodeMode.NONE;
    private BarcodeCallback callback = null;
    private DecoderThread decoderThread;

    private DecoderFactory decoderFactory;
    private CameraBrightnessCallback cameraBrightnessCallback;


    private Handler resultHandler;

    // TODO: 2019/4/26  result
    private final Handler.Callback resultCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == R.id.zxing_id_decode_succeeded) {
                BarcodeResult result = (BarcodeResult) message.obj;

                if (result != null) {
                    if (callback != null && decodeMode != DecodeMode.NONE) {
                        callback.barcodeResult(result);
                        if (decodeMode == DecodeMode.SINGLE) {
                            stopDecoding();
                        }
                    }
                }
                return true;
            } else if (message.what == R.id.zxing_id_decode_failed) {
                // Failed. Next preview is automatically tried.
                return true;
            } else if (message.what == R.id.zxing_id_possible_result_points) {
                List<ResultPoint> resultPoints = (List<ResultPoint>) message.obj;
                if (callback != null && decodeMode != DecodeMode.NONE) {
                    callback.possibleResultPoints(resultPoints);
                }
                return true;
            }
            return false;
        }
    };


    public BarcodeView(Context context) {
        super(context);
        initialize(context, null);
    }

    public BarcodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public BarcodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }


    private void initialize(Context context, AttributeSet attrs) {
        decoderFactory = new DefaultDecoderFactory();
        resultHandler = new Handler(resultCallback);
    }


    /**
     * Set the DecoderFactory to use. Use this to specify the formats to decode.
     *
     * Call this from UI thread only.
     *
     * @param decoderFactory the DecoderFactory creating Decoders.
     * @see DefaultDecoderFactory
     */
    public void setDecoderFactory(DecoderFactory decoderFactory) {
        Util.validateMainThread();

        this.decoderFactory = decoderFactory;
        if (this.decoderThread != null) {
            this.decoderThread.setDecoder(createDecoder());
        }
    }

    private Decoder createDecoder() {
        if (decoderFactory == null) {
            decoderFactory = createDefaultDecoderFactory();
        }
        DecoderResultPointCallback callback = new DecoderResultPointCallback();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, callback);
        Decoder decoder = this.decoderFactory.createDecoder(hints);
        callback.setDecoder(decoder);
        return decoder;
    }

    /**
     *
     * @return the current DecoderFactory in use.
     */
    public DecoderFactory getDecoderFactory() {
        return decoderFactory;
    }

    /**
     * Decode a single barcode, then stop decoding.
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    public void decodeSingle(BarcodeCallback callback) {
        this.decodeMode = DecodeMode.SINGLE;
        this.callback = callback;
        startDecoderThread();
    }


    /**
     * Continuously decode barcodes. The same barcode may be returned multiple times per second.
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    public void decodeContinuous(BarcodeCallback callback) {
        this.decodeMode = DecodeMode.CONTINUOUS;
        this.callback = callback;
        startDecoderThread();
    }

    public void setCameraBrightnessCallback(
        CameraBrightnessCallback cameraBrightnessCallback) {
        this.cameraBrightnessCallback = cameraBrightnessCallback;
    }

    /**
     * Stop decoding, but do not stop the preview.
     */
    public void stopDecoding() {
        this.decodeMode = DecodeMode.NONE;
        this.callback = null;
        stopDecoderThread();
    }

    protected DecoderFactory createDefaultDecoderFactory() {
        return new DefaultDecoderFactory();
    }

    private void startDecoderThread() {
//        Log.e("time_to", "startDecoderThread: - > " );
        stopDecoderThread(); // To be safe

        if (decodeMode != DecodeMode.NONE && isPreviewActive()) {
            // We only start the thread if both:
            // 1. decoding was requested
            // 2. the preview is active
            decoderThread = new DecoderThread(getCameraInstance(), createDecoder(), resultHandler);
            decoderThread.setCropRect(getFramingRect());
            decoderThread.start();
        }
    }

    @Override
    protected void previewStarted() {
        super.previewStarted();

        startDecoderThread();
        if (decodeMode != DecodeMode.NONE && isPreviewActive()) {

            getCameraInstance().getCameraManager().setDelegate(cameraBrightnessCallback);
        }
    }

    private void stopDecoderThread() {
        if (decoderThread != null) {
            decoderThread.stop();
            decoderThread = null;
        }
    }
    /**
     * Stops the live preview and decoding.
     *
     * Call from the Activity's onPause() method.
     */
    @Override
    public void pause() {
        stopDecoderThread();

        super.pause();
    }
}
