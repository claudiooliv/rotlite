package br.com.rotlite.rotlite;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by claudio on 12/09/15.
 */
public class RotLite {

    private static String TAG = "RotLite";
    private Context context;
    private static RotLite instance;
    private static SQLiteDatabase db;
    private static SQLiteDatabase dbReadable;
    public static String BROADCAST_SYNC_ACTION = "br.com.rotlite.rotlite.BROADCAST_SYNC_ACTION";
    public static String BROADCAST_SYNC_STATUS_SUCCESS = "success";
    public static String BROADCAST_SYNC_STATUS_ERROR = "error";
    public static String BODY_DATA_FORM = "form";
    public static String BODY_DATA_JSON = "json";
    public static final Map<String, Class<? extends RotLiteObject>> classes = new ConcurrentHashMap();
    private static boolean foreignKeyEnabled = false;

    public static RotLite getInstance() {
        if (instance == null) instance = new RotLite();
        return instance;
    }

    public static SQLiteDatabase getDataBase(Context context) {
        if (db == null) db = new RotLiteCore(context).getWritableDatabase();
        if (db != null && !db.isReadOnly() && !foreignKeyEnabled) {
            foreignKeyEnabled = true;
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
        return db;
    }
    public static SQLiteDatabase getReadableDataBase(Context context) {
        if (dbReadable == null) dbReadable = new RotLiteCore(context).getReadableDatabase();
        return dbReadable;
    }

    public static RotLite register(Class<? extends RotLiteObject> clazz) {

        if (clazz != null) {

            String className = clazz.getName();

            if (!classes.containsKey(className)) {
                classes.put(className, clazz);
            }

        }

        return instance;

    }

    public static void startSync(Context context) {

        getDataBase(context);

        if (serviceRunning(context)) {
            Log.v("Rotlite", "service is running");
        }else{
            Log.v("Rotlite", "service is not running");
        }

        Intent sync = new Intent(context, RotLiteSyncIntentService.class);
        context.startService(sync);

        /*
        if (!serviceRunning(context)) {
            context.startService(new Intent(context, RotLiteService.class));
            Log.v(TAG, "Starting service");
        }else{
            Log.v(TAG, "Service is running");
        }*/

    }

    public static boolean isSyncing(){
        return RotLiteSyncIntentService.isRunning;
    }

    private static boolean serviceRunning(Context context) {
        Class<?> serviceClass = RotLiteSyncIntentService.class;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return manager.getActiveNetworkInfo() != null &&
                manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

}
