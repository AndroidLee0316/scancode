package com.pasc.lib.barcodescanner;

import com.pasc.lib.zxing.ResultPoint;

import java.util.List;

/**
 * Callback that is notified when a barcode is scanned.
 */
public interface BarcodeCallback {
    /**
     * Barcode was successfully scanned.
     *
     * @param result the result
     */
    void barcodeResult(com.pasc.lib.barcodescanner.BarcodeResult result);

    /**
     * ResultPoints are detected. This may be called whether or not the scanning was successful.
     *
     * This is mainly useful to give some feedback to the user while scanning.
     *
     * Do not depend on this being called at any specific point in the decode cycle.
     *
     * @param resultPoints points potentially identifying a barcode
     */
    void possibleResultPoints(List<ResultPoint> resultPoints);
}
