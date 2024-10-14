package com.pasc.lib.scanqr.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.pasc.lib.barcodescanner.Decoder;
import com.pasc.lib.barcodescanner.DecoderResultPointCallback;
import com.pasc.lib.barcodescanner.DefaultDecoderFactory;
import com.pasc.lib.zxing.DecodeHintType;
import com.pasc.lib.zxing.RGBLuminanceSource;
import com.pasc.lib.zxing.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ex-huangzhiyi001 on 2019/5/14.
 */
public class ImageDecodeUtil {

    private static final Long SCAN_DEFAULT_SIZE = 4000000L;

    public static Result decode (Context context, Intent data) {
        Uri contentUri = data.getData();
        String filePath = "$$";
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt == 19 && DocumentsContract.isDocumentUri(context, contentUri)) {
            String wholeID = DocumentsContract
                    .getDocumentId(contentUri);
            String id = wholeID.split(":")[1];
            String[] column = {MediaStore.Images.Media.DATA};
            String sel = MediaStore.Images.Media._ID + "=?";
            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column, sel, new String[]{id}, null);
            if (null == cursor) {
                //图片未找到
                return null;
            }
            int columnIndex = cursor.getColumnIndex(column[0]);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        } else if (contentUri != null){
            if (!TextUtils.isEmpty(contentUri.getAuthority())) {
                Cursor cursor = context.getContentResolver().query(contentUri,
                        new String[]{MediaStore.Images.Media.DATA},
                        null, null, null);
                if (null == cursor) {
                    //图片未找到
                    return null;
                }
                cursor.moveToFirst();
                filePath = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));
                cursor.close();
            } else {
                if (null != data.getData()){
                    filePath = data.getData().getPath();
                }
            }
        }

        Bitmap b = compress(filePath);
        if (null == b){
            return null;
        }
        int[] ints = new int[b.getWidth()*b.getHeight()];
        b.getPixels(ints, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
        RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(b.getWidth(), b.getHeight(),
                ints);
//            //把可视图片转为二进制图片
//            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(rgbLuminanceSource));
        //解析图片中的code
        Result result = createDecode().decode(rgbLuminanceSource);
        return result;
    }

    public static Result decode (Context context, String filePath) {

        Bitmap b = compress(filePath);
        if (null == b){
            return null;
        }
        int[] ints = new int[b.getWidth()*b.getHeight()];
        b.getPixels(ints, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
        RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(b.getWidth(), b.getHeight(),
            ints);
        //            //把可视图片转为二进制图片
        //            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(rgbLuminanceSource));
        //解析图片中的code
        Result result = createDecode().decode(rgbLuminanceSource);
        return result;
    }

    private static Decoder createDecode(){
        DefaultDecoderFactory decoderFactory = new DefaultDecoderFactory();
        DecoderResultPointCallback callback = new DecoderResultPointCallback();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, callback);
        Decoder decoder = decoderFactory.createDecoder(hints);
        callback.setDecoder(decoder);
        return decoder;
    }

    private static Bitmap compress(String srcPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath);
        if (null == bitmap){
            return null;
        }
        if (bitmap.getByteCount() > SCAN_DEFAULT_SIZE){
            //大图
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            //开始读入图片，此时把options.inJustDecodeBounds 设回true了
            newOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(srcPath, newOpts);//此时返回bm为空

            newOpts.inJustDecodeBounds = false;
            int w = newOpts.outWidth;
            int h = newOpts.outHeight;
            //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
            float hh = 800f;//这里设置高度为800f           这里我写死了尺寸
            float ww = 480f;//这里设置宽度为480f           这里我写死了尺寸
            //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
            int be = 1;//be=1表示不缩放
            if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
                be = (int) (newOpts.outWidth / ww);
            } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
                be = (int) (newOpts.outHeight / hh);
            }
            if (be <= 0){
                be = 1;
            }
            newOpts.inSampleSize = be;//设置缩放比例
            //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
            bitmap.recycle();
            bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
            return bitmap;
        } else{
            //小图  true表示要不用压缩
            return bitmap;
        }
    }
}
