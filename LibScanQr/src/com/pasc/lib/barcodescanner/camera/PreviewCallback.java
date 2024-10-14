package com.pasc.lib.barcodescanner.camera;

import com.pasc.lib.barcodescanner.SourceData;

/**
 * Callback for camera previews.
 */
public interface PreviewCallback {
    void onPreview(SourceData sourceData);
}
