package com.pasc.lib.zxing;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多线程解析图像数据
 * Created by ex-huangzhiyi001 on 2019/4/28.
 */
public class MultiResultHandler {
  private static final String TAG = "MultiResultHandler";
  private HT hT;
  private ExecutorService mExecutor = Executors.newCachedThreadPool();
  private ScheduledExecutorService mSingleExecutor = Executors.newSingleThreadScheduledExecutor();
  private BinaryBitmap mImage;
  private Map mHints = new HashMap<>();
  private Handler mHandler;
  private final int result_code = 0x233;
  private Reader[] mReaders = null;
  private Collection<Future<?>> mFutures = new ArrayList<>();
  private int count = 0;
  private Result result;
  private Runnable mCountRunnable = new Runnable() {
    @Override
    public void run() {
      //改变状态
      handlerNotNull = false;

      //清理掉
      Iterator<Future<?>> iterator = mFutures.iterator();
      while (iterator.hasNext()) {
        Future<?> f = iterator.next();
        if (!f.isDone()) {
          f.cancel(true);
        }
      }
      //for (Future<?> f : mFutures) {
      //  if (!f.isDone()) {
      //    f.cancel(true);
      //  }
      //}
      //清除handler
      //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
      //    hT.quitSafely();
      //}else {
      //    hT.quit();
      //}
      //hT = null;
    }
  };
  private boolean handlerNotNull = false;
  private ScheduledFuture<?> mCountSchedule;
  private long pre_time;

  private static class SingletonHolder {
    private static final MultiResultHandler INSTANCE = new MultiResultHandler();
  }

  public static MultiResultHandler getInstance() {
    return MultiResultHandler.SingletonHolder.INSTANCE;
  }

  private MultiResultHandler() {
    //checkHandler();
  }

  private void checkHandler() {
    long sys_time = System.currentTimeMillis();
    //500ms 内定时器不起作用
    if (sys_time - pre_time < 500 && handlerNotNull) {
      pre_time = sys_time;
      return;
    }
    pre_time = sys_time;
    //离开后15s，释放线程资源
    if (handlerNotNull) {
      if (!mCountSchedule.isDone()) {
        mCountSchedule.cancel(true);
      }
      mCountSchedule = null;
      //重新计时
      mCountSchedule = mSingleExecutor.schedule(mCountRunnable, 15, TimeUnit.SECONDS);
      //Log.e(TAG, "checkHandler: old ht -> "+hT );
    } else {
      //null
      mCountSchedule = mSingleExecutor.schedule(mCountRunnable, 15, TimeUnit.SECONDS);
      hT = new HT("MultiResultHandler");
      hT.start();
      mHandler = new Handler(hT.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
          if (msg.what == result_code) {
            if (msg.obj != null) {
              result = (Result) msg.obj;
              //有结果了
              //清理掉
              //for (Future<?> f : mFutures) {
              //  if (!f.isDone()) {
              //    f.cancel(true);
              //  }
              //}
              Iterator<Future<?>> iterator = mFutures.iterator();
              while (iterator.hasNext()) {
                Future<?> f = iterator.next();
                if (!f.isDone()) {
                  f.cancel(true);
                }
              }
              accomplish();
              //Log.e(TAG, "handleMessage: has" );
              return;
            }
            //版本太高，不合适用
            //hT.getLooper().getQueue().isIdle()
            //用计数器
            count++;
            if (null != mReaders && count == mReaders.length) {
              //刚好
              //清理掉
              Iterator<Future<?>> iterator = mFutures.iterator();
              while (iterator.hasNext()) {
                Future<?> f = iterator.next();
                if (!f.isDone()) {
                  f.cancel(true);
                }
              }
              //for (Future<?> f : mFutures){
              //    if (!f.isDone()){
              //        f.cancel(true);
              //    }
              //}
              accomplish();
              //Log.e(TAG, "handleMessage: null" );
            }
          }
        }
      };
      //Log.e(TAG, "checkHandler: new ht -> "+hT );
      handlerNotNull = true;
    }
  }

  private void accomplish() {
    if (hT == null) {
      return;
    }
    //notify
    synchronized (hT) {
      hT.notify();
    }
    //清理
    count = 0;
    mFutures.clear();
    mHandler.removeCallbacksAndMessages(null);

    this.mImage = null;
    this.mReaders = null;
    this.mHints.clear();
  }

  public void resetReader(final Reader[] readers) {
    for (final Reader reader : readers) {
      mExecutor.submit(new TimerTask() {
        @Override
        public void run() {
          reader.reset();
        }
      });
    }
  }

  public synchronized Result decodeInternal(BinaryBitmap image, Reader[] readers,
      Map<DecodeHintType, ?> hints) {
    checkHandler();
    result = null;
    if (Looper.getMainLooper() == Looper.myLooper()) {
      //主线程，异常
      throw new IllegalStateException("不能在主线程处理...");
    }
    this.mImage = image;
    this.mReaders = readers;
    this.mHints.putAll(hints);
    if (mReaders != null) {
      for (final Reader reader : mReaders) {
        Future<?> submit = mExecutor.submit(new TimerTask() {
          @Override
          public void run() {
            Message obtain = Message.obtain();
            obtain.what = result_code;
            try {
              Result decode = reader.decode(mImage, mHints);
              obtain.obj = decode;
            } catch (ReaderException re) {
              // stop
              //不用处理
            } finally {
              mHandler.sendMessage(obtain);
            }
          }
        });
        mFutures.add(submit);
      }
    }
    //waiting
    synchronized (hT) {
      try {
        hT.wait();
      } catch (InterruptedException e) {
        //Log.e(TAG, "decodeInternal: end------ e -> " + e.getMessage());
      }
    }
    //        if (result != null){
    //            Log.e(TAG, "decodeInternal: end------have");
    //        }
    //Log.e(TAG, "decodeInternal: end------");
    return result;
  }

  class HT extends HandlerThread {

    private HT(String name) {
      super(name);
    }
  }
}
