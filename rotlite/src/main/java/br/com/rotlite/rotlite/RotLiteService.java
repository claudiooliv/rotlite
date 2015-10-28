package br.com.rotlite.rotlite;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by claudio on 12/09/15.
 */
public class RotLiteService extends Service {

    public static String TAG = RotLiteService.class.getSimpleName();

    @Override
    public IBinder onBind(Intent arg0) {
        Log.v(TAG, "onStartCommand");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        //return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
    }
}
