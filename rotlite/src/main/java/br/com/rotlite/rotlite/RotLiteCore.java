package br.com.rotlite.rotlite;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by claudio on 08/08/15.
 */
public class RotLiteCore extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    public static String TAG = "RotLiteCore";
    public RotLiteCore instance;

    public RotLiteCore(Context context) {
        super(context, getMeta(context, "rotlite_dbname"), null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("CREATE TABLE rotlite_objects (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE rotlite_objects");
        //onCreate(db);
    }

    public static String getMeta(Context context, String name) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.get(name).toString();

        } catch (PackageManager.NameNotFoundException e) {
            //Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            //Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }
        return null;
    }
}