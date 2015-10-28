package br.com.rotlite.rotlite;

import com.squareup.okhttp.Callback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by claudio on 08/08/15.
 */
public interface RotLiteInterface<T extends RotLiteObject> {

    void setAutoSync(boolean autosync);
    void include(String column);
    boolean getAutoSync();
    void setTbName(String name);
    String getTbName();
    void setId(String id);
    void setLocalId(int id);
    long getLocalId();
    String getId();
    boolean hasId();
    void configEndpoints();
    void setBodyDataFormat(String format);
    String getBodyDataFormat();
    void setEndPointParam(String param, String value);
    void setEndPointPostParam(String param, String value);
    void setEndPointPutParam(String param, String value);
    void setEndPointDeleteParam(String param, String value);
    void put(String key, String value);
    void put(String key, double value);
    void put(String key, int value);
    void put(String key, long value);
    void put(String key, boolean value);
    void put(String key, JSONObject value);
    void put(String key, JSONArray value);
    void putDate(String key, String value);
    void putDateTime(String key, String value);
    void putNull(String key);
    String getString(String key);
    double getDouble(String key);
    int getInt(String key);
    long getLong(String key);
    boolean getBoolean(String key);
    JSONObject getJSONObject(String key);
    JSONArray getJSONArray(String key);
    String getDate(String key);
    String getDateTime(String key);
    boolean has(String key);
    void beforeSaveLocal();
    void saveLocal() throws Exception;
    void beforeSaveWeb();
    void saveWeb(RotLiteSyncUploadCallback callback) throws RotLiteException;
    //void saveWeb(Callback callback) throws Exception;
    void save(RotLiteSyncCallback callback) throws Exception;
    boolean tbExists(String tbname);
    void createTable(String tbname) throws Exception;
    void where(String where);
    List<T> find() throws RotLiteException;
    void findInBackground(RotLiteCallback callback);
    void limit(int max);
    void limit(int min, int max);
    JSONObject jsonObject();
    String jsonString();
    void getById(String uuid);
    String getExecutedQuery();
    void order(String order);
    void group(String column);
    boolean delete();
    boolean update();
    void showActivityLogs(boolean activity);
    void fromLocal();
    void fromWeb();
    void setEndpoint(String name);
    void setEndpointPost(String name);
    void setEndpointPut(String name);
    void setEndpointDelete(String name);
    String getEndpoint();
    //void getObject() throws RotLiteException;
    //void getObjectInBackground() throws RotLiteException;
    //<T extends RotLiteObject> T getObject(String column);
    void setDateFormat(String format);
    String getDateFormat();
    void setConverter(RotLiteConverter converter);
    RotLiteConverter getConverter();
    void setDefaultIdFieldName(String name);
    String getDefaultIdFieldName();
    void prepareSync();
    void onReceiveSyncData(List<T> data);
    List<T> beforeSync(List<T> list);
    void onSync(List<T> synchronizedList);
    void onSync(List<T> synchronizedList, List<T> unsynchronizedList);
    void sync() throws RotLiteException;
    void syncInBackground(SyncCallback callback);
    String getLastSyncDate();
    void setLastSyncDate(String date);
    boolean isSync();
    void setIsSync(Boolean isSync);
}
