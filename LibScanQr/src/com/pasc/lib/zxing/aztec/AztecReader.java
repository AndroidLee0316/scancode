/*
 * Copyright 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pasc.lib.zxing.aztec;

import com.pasc.lib.zxing.BarcodeFormat;
import com.pasc.lib.zxing.BinaryBitmap;
import com.pasc.lib.zxing.DecodeHintType;
import com.pasc.lib.zxing.FormatException;
import com.pasc.lib.zxing.NotFoundException;
import com.pasc.lib.zxing.Reader;
import com.pasc.lib.zxing.Result;
import com.pasc.lib.zxing.ResultMetadataType;
import com.pasc.lib.zxing.ResultPoint;
import com.pasc.lib.zxing.ResultPointCallback;
import com.pasc.lib.zxing.aztec.decoder.Decoder;
import com.pasc.lib.zxing.aztec.detector.Detector;
import com.pasc.lib.zxing.common.DecoderResult;
import com.pasc.lib.zxing.BarcodeFormat;
import com.pasc.lib.zxing.BinaryBitmap;
import com.pasc.lib.zxing.DecodeHintType;
import com.pasc.lib.zxing.FormatException;
import com.pasc.lib.zxing.NotFoundException;
import com.pasc.lib.zxing.Reader;
import com.pasc.lib.zxing.Result;
import com.pasc.lib.zxing.ResultMetadataType;
import com.pasc.lib.zxing.ResultPoint;
import com.pasc.lib.zxing.ResultPointCallback;
import com.pasc.lib.zxing.aztec.decoder.Decoder;
import com.pasc.lib.zxing.aztec.detector.Detector;
import com.pasc.lib.zxing.common.DecoderResult;

import java.util.List;
import java.util.Map;

/**
 * This implementation can detect and decode Aztec codes in an image.
 *
 * @author David Olivier
 */
public final class AztecReader implements Reader {

  /**
   * Locates and decodes a Data Matrix code in an image.
   *
   * @return a String representing the content encoded by the Data Matrix code
   * @throws NotFoundException if a Data Matrix code cannot be found
   * @throws FormatException if a Data Matrix code cannot be decoded
   */
  @Override
  public Result decode(BinaryBitmap image) throws NotFoundException, FormatException {
    return decode(image, null);
  }

  @Override
  public Result decode(BinaryBitmap image, Map<DecodeHintType,?> hints)
      throws NotFoundException, FormatException {

    NotFoundException notFoundException = null;
    FormatException formatException = null;
    Detector detector = new Detector(image.getBlackMatrix());
    ResultPoint[] points = null;
    DecoderResult decoderResult = null;
    try {
      AztecDetectorResult detectorResult = detector.detect(false);
      points = detectorResult.getPoints();
      decoderResult = new Decoder ().decode(detectorResult);
    } catch (NotFoundException e) {
      notFoundException = e;
    } catch (FormatException e) {
      formatException = e;
    }
    if (decoderResult == null) {
      try {
        AztecDetectorResult detectorResult = detector.detect(true);
        points = detectorResult.getPoints();
        decoderResult = new Decoder().decode(detectorResult);
      } catch (NotFoundException | FormatException e) {
        if (notFoundException != null) {
          throw notFoundException;
        }
        if (formatException != null) {
          throw formatException;
        }
        throw e;
      }
    }

    if (hints != null) {
      ResultPointCallback rpcb = (ResultPointCallback) hints.get(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
      if (rpcb != null) {
        for (ResultPoint point : points) {
          rpcb.foundPossibleResultPoint(point);
        }
      }
    }

    Result result = new Result(decoderResult.getText(),
                               decoderResult.getRawBytes(),
                               decoderResult.getNumBits(),
                               points,
                               BarcodeFormat.AZTEC,
                               System.currentTimeMillis());

    List<byte[]> byteSegments = decoderResult.getByteSegments();
    if (byteSegments != null) {
      result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
    }
    String ecLevel = decoderResult.getECLevel();
    if (ecLevel != null) {
      result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
    }

    return result;
  }

  @Override
  public void reset() {
    // do nothing
  }

}
