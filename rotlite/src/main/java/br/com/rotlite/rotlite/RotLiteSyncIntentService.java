package br.com.rotlite.rotlite;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.squareup.okhttp.Response;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by claudio on 29/09/15.
 */
public class RotLiteSyncIntentService<T extends RotLiteObject> extends IntentService {

    public static String TAG = RotLiteSyncIntentService.class.getSimpleName();
    private Map<String, Class<? extends RotLiteObject>> classes = new ConcurrentHashMap();

    public RotLiteSyncIntentService() {
        super("RotLiteSyncIntentService");
    }

    public static boolean isRunning = false;

    List<RotLiteModel> objsToUpload;

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("Tasky", "Iniciando Sincronização - RotLib - " + isRunning);
        isRunning = true;
        try {
            Log.i("Tasky", "Downloading Data");
            download();
            Log.i("Tasky", "Uploading Data");
            upload();
        } finally {
            Log.i("Tasky", "Finalizando Sincronização - RotLib");
//            isRunning = false;
        }
    }

    private void upload() {

        classes = RotLite.getInstance().classes;

        final RotLiteModel toUpload = new RotLiteModel(getApplicationContext());
        toUpload.findInBackground(new RotLiteCallback<RotLiteModel>() {
            @Override
            public void done(List<RotLiteModel> list) {
                objsToUpload = list;
                uploadObjs(list);
            }

            @Override
            public void error(RotLiteException e) {

            }
        });

    }

    private void uploadObjs(List<RotLiteModel> list) {
        Log.v(TAG, "To upload: " + list.size());
        for (int i = list.size() - 1; i >= 0; i--) {
            RotLiteModel m = list.get(i);

            final int pos = i;

            final String modelName = m.getString("model");
            final String objectId = m.getString("data_id");

            Log.v(TAG, "saveweb " + objectId);

            for (Map.Entry<String, Class<? extends RotLiteObject>> entry : classes.entrySet()) {

                if (entry.getKey().equals(modelName)) {

                    final Class<? extends RotLiteObject> clzz = entry.getValue();
                    final T model;
                    try {
                        model = (T) clzz.getDeclaredConstructor(Context.class).newInstance(getApplication());

                        model.getById(objectId);

                        try {
                            List<T> find = model.find();
                            if (find.size() > 0) {
                                final T obj = (T) find.get(0);
                                obj.setIsSync(true);
                                obj.saveWeb(new RotLiteSyncUploadCallback() {
                                    @Override
                                    public void onFailure(RotLiteException e) {
                                        e.printStackTrace();
                                        Log.e(TAG, "Failure to upload '" + obj.getId() + "': " + e.getMessage() + " - " + e.hashCode());
                                        if (objsToUpload.size() > pos) objsToUpload.remove(pos);
                                    }

                                    @Override
                                    public void onSuccess(Response response, RotLiteObject objUpload) {
                                        RotLiteModel temp = new RotLiteModel(getApplicationContext());
                                        temp.where("model = '" + modelName + "' and data_id = '" + objectId + "'");
                                        temp.delete();

                                        obj.setIsSync(true);
                                        if (obj.update()) {
                                            Log.v("setSync", "ops1 ");
                                        }else{
                                            Log.v("setSync", "opa..");
                                        }
                                        if (objsToUpload.size() >= pos) objsToUpload.remove(pos);

                                        if (objsToUpload.size() <= 0) {
                                            objsToUpload.clear();
                                            Intent in = new Intent(RotLite.BROADCAST_SYNC_ACTION);
                                            in.putExtra("model", model.getClass().getName());
                                            in.putExtra("action", "upload");
                                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(in);
                                        }

                                        Log.v(TAG, "Success to upload '" + objUpload.getId() + "': " + objUpload.jsonString());
                                    }
                                });
                            }
                        } catch (RotLiteException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Error: " + e.getMessage());
                        }



                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                }

            }

        }
    }

    private void download() {
        classes = RotLite.getInstance().classes;

        for (Map.Entry<String, Class<? extends RotLiteObject>> entry : classes.entrySet()) {

            final Class<? extends RotLiteObject> clzz = entry.getValue();
            final T model;
            try {

                //Trazendo dados da web

                model = (T) clzz.getDeclaredConstructor(Context.class).newInstance(getApplication());
                final boolean autosync = model.getAutoSync();

                model.prepareSync(); //Se tivermos que executar algo antes da sync, isso acontecerá aqui!
                model.fromWeb();

                Log.d(TAG, "Gonna find inda web");

                model.findInBackground(new RotLiteCallback<RotLiteObject>() {
                    @Override
                    public void done(List<RotLiteObject> list) {
                        Log.d(TAG, String.format("Found inda web %d", list != null ? list.size(): 0));
                        if (list == null) list = new ArrayList<>();

                        List<T> aux = new ArrayList<>();

                        for (int i = list.size() - 1; i >= 0; i--) {
                            RotLiteObject obj = list.get(i);
                            list.remove(i);
                            try {
                                T newModel;
                                try {
                                    newModel = (T) clzz.getDeclaredConstructor(Context.class).newInstance(getApplication());
                                    newModel.content = obj.content;
                                    newModel.dataType = obj.dataType;
                                    newModel.setId(obj.getId());
                                    newModel.setIsSync(true);
                                    aux.add(newModel);
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                } catch (NoSuchMethodException e) {
                                    e.printStackTrace();
                                }
                            }finally {
                                obj = null;
                            }
                        }

                        if (!autosync) {

                            model.onReceiveSyncData(aux);

                        } else {

                            List<T> objs = model.beforeSync(aux);
                            //aux.clear();

                            if (objs != null) {

                                if (objs.size() != 0) {
//                                    List<T> synchronizedObjs = new ArrayList<>();
//                                    List<T> unsynchronizedObjs = new ArrayList<>();
                                    for (int i = 0; i < objs.size(); i++) {

                                        T obj = objs.get(i);

                                        obj.setIsSync(true);

                                        try {
                                            obj.saveLocal();
//                                            synchronizedObjs.add(obj);
                                        } catch (Exception e) {
//                                            unsynchronizedObjs.add(obj);
                                            e.printStackTrace();
                                        }

                                    }

                                    java.text.DateFormat dateFormat = new SimpleDateFormat(model.getDateFormat());
                                    Date date = new Date();

                                    model.setLastSyncDate(dateFormat.format(date));
                                    model.onSync(null);
//                                    if (unsynchronizedObjs.size() > 0)
//                                        model.onSync(synchronizedObjs, unsynchronizedObjs);

                                    Intent in = new Intent(RotLite.BROADCAST_SYNC_ACTION);
                                    in.putExtra("model", model.getClass().getName());
                                    in.putExtra("action", "download");
                                    in.putExtra("status", RotLite.BROADCAST_SYNC_STATUS_SUCCESS);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(in);

                                    aux.clear();
                                    objs.clear();

                                } else {
                                    Log.e(TAG, "Nenhum objeto retornado em beforeSync. Sincronização não concluída. 1");
                                }
                            } else {
                                Log.e(TAG, "Nenhum objeto retornado em beforeSync. Sincronização não concluída. 2");
                                    
                            }

                        }
                    }

                    @Override
                    public void error(RotLiteException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage() + " - " + e.hashCode());
                        Intent in = new Intent(RotLite.BROADCAST_SYNC_ACTION);
                        in.putExtra("model", model.getClass().getName());
                        in.putExtra("action", "download");
                        in.putExtra("status", RotLite.BROADCAST_SYNC_STATUS_ERROR);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(in);
                    }
                });

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

        }
    }
}
