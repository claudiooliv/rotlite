package br.com.rotlite.rotlite;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import br.com.rotlite.rotlite.utils.LoggingInterceptor;

/**
 * Created by claudio on 08/08/15.
 */
public class RotLiteObject<T extends RotLiteObject> implements RotLiteInterface, Serializable {

    String TAG1 = "MADRUGA";

    private final String ROTLITE_IS_SYNC = "rotlite_is_sync";
    private Table table;
    public String name, className;
    ContentValues content;
    String TAG = "RotLiteObject";
    String TAG_ACTIVITY = "RotLiteObjectActivity";
    private List<String> columnTypes = new ArrayList<>();
    private Context context;

    private final OkHttpClient client;
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private RotLiteCore dbcore;
    private SQLiteDatabase db;
    private String _id = "";
    private long localId = 0;
    private Class<T> classInstance;
    public static String classInstanceName;
    private String where = "1 = 1";
    private int limitMin = 0;
    private int limitMax = 0;
    private String limit = "";
    private String query = "";
    private String order = "";
    private String group = "";
    private String url = "";
    private String endpoint = "";
    private String endpointPost = "";
    private String endpointPut = "";
    private String endpointDelete = "";
    private int method = 0;
    private boolean activityLogs = false;
    private boolean autosync = false;
    private int from;
    Map<String, String> dataType = new HashMap<String, String>();
    private static String MODEL_TAG = "model";
    private boolean customWebService = false;
    private static String authKey = "";
    private boolean getById = false;
    public static String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private RotLiteConverter converter;
    public String DEFAULT_ID_FIELD_NAME = "uuid";
    public SharedPreferences sPrefs;
    public static RotLiteBroadcastReceiver broadcastReceiver;
    public String bodyDataFormat = RotLite.BODY_DATA_JSON;
    private boolean updateWebError = false;
    //Define a páginação atual da busca
    private int currentPage = 1;
    private Map<String, ForeignKeyContent> foreignKeys = new HashMap<String, ForeignKeyContent>();
    private List<String> include = new ArrayList();
    public Map<String, T> includedModels = new HashMap<String, T>();

    public RotLiteObject() {
        client = new OkHttpClient();
    }

    public RotLiteObject(Context context, Class<T> obj) {
        client = new OkHttpClient();
        //Manter comentado, pois causa estouro de memória -> LG l5 TODO: deixar melhor essa config
        //client.interceptors().add(new LoggingInterceptor());
        this.context = context;
        db = RotLite.getInstance().getDataBase(context);
        url = RotLiteCore.getMeta(this.context, "rotlite_server");
        authKey = RotLiteCore.getMeta(this.context, "rotlite_auth_key");
        //db = dbcore.getWritableDatabase();
        this.classInstance = obj;
        classInstanceName = obj.getName();

        className = this.classInstance.getName();

        if (this.classInstance.isAnnotationPresent(Table.class)) {

            Annotation annotation = this.classInstance.getAnnotation(Table.class);
            table = (Table) annotation;

            this.name = table.name();
            this.endpoint = table.endpoint();
            this.endpointPost = table.endpointPost();
            this.endpointPut = table.endpointPut();
            this.endpointDelete = table.endpointDelete();
            this.autosync = table.autosync();

            if (!this.endpoint.equals("")) {
                this.customWebService = true;
            }

            sPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        }

        for(Field field : this.classInstance.getDeclaredFields()){
            Class type = field.getType();
            String name = field.getName();
            Annotation[] annotations = field.getDeclaredAnnotations();

            if (field.isAnnotationPresent(ForeignKey.class)) {

                Class superClass = type.getSuperclass();

                //Log.v("rotLiteKey", "TEM FOREIGN KEY!! Name: " + name + "; Type: " + type + "; Super: " + type.getSuperclass());
                if (type.isAssignableFrom(RotLiteObject.class) || superClass.equals(RotLiteObject.class) || superClass.getSuperclass().isAssignableFrom(RotLiteObject.class)) {

                    for (Annotation annotation : annotations) {

                        ForeignKey fk = (ForeignKey) annotation;
                        foreignKeys.put(fk.column(), new ForeignKeyContent(type, fk.column(), fk.references(), this.context));

                    }

                }
            }
        }

        TAG = this.classInstance.getSimpleName();
        MODEL_TAG = "RotLite_" + TAG;

        this.from = RotLiteConsts.FROM_LOCAL;

    }

    private String getUrl() {
        return url;
    }

    @Override
    public void setAutoSync(boolean autosync) {
        this.autosync = autosync;
    }

    @Override
    public void include(String column) {

        if (foreignKeys.containsKey(column)) {

            include.add(column);

        }else{
            Log.e(TAG, "A coluna '" + column + "' não possui uma Foreign Key definida");
        }

    }

    @Override
    public boolean getAutoSync() {
        return autosync;
    }

    @Override
    public void setTbName(String name) {
        this.name = name;
    }

    @Override
    public String getTbName() {
        return name;
    }

    private void setId() {
        if (_id.equals("")) _id = generateUUID();
        if (!content.containsKey(DEFAULT_ID_FIELD_NAME)) put(DEFAULT_ID_FIELD_NAME, _id);
    }

    @Override
    public void setId(String id) {
        this._id = id;
        if (content != null && !content.containsKey(DEFAULT_ID_FIELD_NAME))
            put(DEFAULT_ID_FIELD_NAME, _id);
    }

    @Override
    public void setLocalId(int id) {
        localId = id;
        if (content != null && !content.containsKey("_id")) put("_id", localId);
    }

    @Override
    public long getLocalId() {
        return localId;
    }

    @Override
    public String getId() {

        if (content != null && content.containsKey(DEFAULT_ID_FIELD_NAME)) {
            _id = content.getAsString(DEFAULT_ID_FIELD_NAME);
        }

        if (!hasId()) _id = generateUUID();
        return this._id;
    }

    @Override
    public boolean hasId() {
        return ((_id == null || _id.equals("")) ? false : true);
    }

    @Override
    public void configEndpoints() {

    }

    @Override
    public void setBodyDataFormat(String format) {
        bodyDataFormat = format;
    }

    @Override
    public String getBodyDataFormat() {
        return bodyDataFormat;
    }

    @Override
    public void setEndPointParam(String param, String value) {
        if (this.endpoint != null && this.url != null) {
            this.endpoint = this.endpoint.replace(":" + param, value);
            this.url = this.url.replace(":" + param, value);
        }
    }

    @Override
    public void setEndPointPostParam(String param, String value) {
        if (this.endpointPost != null && this.url != null) {
            this.endpointPost = this.endpointPost.replace(":" + param, value);
            this.url = this.url.replace(":" + param, value);
        }
    }

    @Override
    public void setEndPointPutParam(String param, String value) {
        if (this.endpointPut != null && this.url != null) {
            this.endpointPut = this.endpointPut.replace(":" + param, value);
            this.url = this.url.replace(":" + param, value);
        }
    }

    @Override
    public void setEndPointDeleteParam(String param, String value) {
        if (this.endpointDelete != null && this.url != null) {
            this.endpointDelete = this.endpointDelete.replace(":" + param, value);
            this.url = this.url.replace(":" + param, value);
        }
    }

    @Override
    public void put(String key, String value) {
        if (content == null) content = new ContentValues();
        content.put(key, value);
        dataType.put(key, RotLiteConsts.DATA_TYPE_STRING);
    }

    @Override
    public void put(String key, double value) {
        if (content == null) content = new ContentValues();
        content.put(key, value);
        dataType.put(key, RotLiteConsts.DATA_TYPE_DOUBLE);
    }

    @Override
    public void put(String key, int value) {
        if (content == null) content = new ContentValues();
        content.put(key, value);
        dataType.put(key, RotLiteConsts.DATA_TYPE_INTEGER);
    }

    @Override
    public void put(String key, long value) {
        if (content == null) content = new ContentValues();
        content.put(key, value);
        dataType.put(key, RotLiteConsts.DATA_TYPE_LONG);
    }

    @Override
    public void put(String key, boolean value) {
        if (content == null) content = new ContentValues();
        content.put(key, value);
        dataType.put(key, RotLiteConsts.DATA_TYPE_BOOLEAN);
    }

    @Override
    public void put(String key, JSONObject value) {

        put(key, value.toString());

    }

    @Override
    public void put(String key, JSONArray value) {

        put(key, value.toString());

    }

    @Override
    public void putDate(String key, String value) {
        if (content == null) content = new ContentValues();
        content.put(key, value);
        dataType.put(key, RotLiteConsts.DATA_TYPE_DATE);
    }

    @Override
    public void putDateTime(String key, String value) {
        if (content == null) content = new ContentValues();
        content.put(key, value);
        dataType.put(key, RotLiteConsts.DATA_TYPE_DATETIME);
    }

    @Override
    public void putNull(String key) {
        if (content == null) content = new ContentValues();
        content.putNull(key);
        dataType.put(key, RotLiteConsts.DATA_TYPE_STRING);
    }

    @Override
    public String getString(String key) {
        return ((has(key) && content.getAsString(key) != null) ? content.getAsString(key) : null);
    }

    @Override
    public double getDouble(String key) {
        return ((has(key) && content.getAsDouble(key) != null) ? content.getAsDouble(key) : 0);
    }

    @Override
    public int getInt(String key) {
        return ((has(key) && content.getAsInteger(key) != null) ? content.getAsInteger(key) : 0);
    }

    @Override
    public long getLong(String key) {
        return ((has(key) && content.getAsLong(key) != null) ? content.getAsLong(key) : 0);
    }

    @Override
    public boolean getBoolean(String key) {
        return ((has(key) && content.getAsBoolean(key) != null) ? content.getAsBoolean(key) : false);
    }

    @Override
    public JSONObject getJSONObject(String key) {
        try {
            return ((has(key) && content.getAsString(key) != null) ? (new JSONObject(content.getAsString(key))) : null);
        } catch (JSONException e) {
            e.printStackTrace();
            if (e != null) Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public JSONArray getJSONArray(String key) {
        try {
            return ((has(key) && content.getAsString(key) != null) ? (new JSONArray(content.getAsString(key))) : null);
        } catch (JSONException e) {
            e.printStackTrace();
            if (e != null) Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public String getDate(String key) {
        return ((has(key) && content.getAsString(key) != null) ? content.getAsString(key) : null);
    }

    @Override
    public String getDateTime(String key) {
        return ((has(key) && content.getAsString(key) != null) ? content.getAsString(key) : null);
    }

    @Override
    public boolean has(String key) {
        return ((content != null && content.containsKey(key)) ? true : false);
    }

    @Override
    public void beforeSaveLocal() {

    }

    @Override
    public void saveLocal() throws Exception {

        this.beforeSaveLocal();

        if (!db.isOpen()) {
            db = RotLite.getInstance().getDataBase(context);
        }

        method = RotLiteConsts.METHOD_INSERT;

        if (content != null && content.containsKey("_id")) {
            content.remove("_id");
        }

        try {

            if (!update()) {

                method = RotLiteConsts.METHOD_INSERT;

                createdAt();
                setId();

                if (!has(ROTLITE_IS_SYNC)) {
                    setIsSync(false);
                }

                localId = db.insertOrThrow(name, null, content);
                if (activityLogs) Log.v(TAG, "Data inserted into the table '" + name + "', data: " + jsonString());

            } else {
                method = RotLiteConsts.METHOD_UPDATE;
                Log.v(TAG, "Atualizou o objeto " + _id + " da tabela '" + name + "'");
            }
        } catch (SQLiteDatabaseCorruptException e) {
            throw e;
            //Log.e(TAG, "SQLiteDatabaseCorruptException Code: " + e.hashCode() + "; Message: " + e.getMessage().toString());
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            //e.printStackTrace();
            throw e;
            //Log.e(TAG, "SQLiteConstraintException saveLocal(): " + e.getMessage() + " --- " + jsonString());
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLiteException saveLocal(): " + e.getMessage());
            String error = e.getMessage();

            if (error.contains(RotLiteConsts.ROTLITE_ERROR_NO_SUCH_TABLE)) {

                createTable(name);
                saveLocal();

                /*

                if () {
                    saveLocal();
                } else {
                    throw new SQLiteException("Não foi possível criar a tabela '" + name + "'");
                }

                */

            } else if (error.contains(RotLiteConsts.ROTLITE_ERROR_HAS_NO_COLUMN)) {

                String getColumn = error.substring(error.indexOf(RotLiteConsts.ROTLITE_ERROR_HAS_NO_COLUMN)
                        + RotLiteConsts.ROTLITE_ERROR_HAS_NO_COLUMN.length() + 1, error.length());

                getColumn = getColumn.substring(0, getColumn.indexOf(" "));
                //TODO: No Galaxy Trend, houve erro ao criar coluna porque o nome estava concatenado com ":". Entender melhor o porque
                getColumn = getColumn.replace(":", "").trim();

                if (foreignKeys.containsKey(getColumn)) {
                    Log.v("rotliteKey", getColumn + " faz parte de um relacionamento 1");
                }

                db.execSQL("ALTER TABLE " + name + " ADD COLUMN " + getColumn + "");
                saveLocal();

            } else if (error.contains(RotLiteConsts.ROTLITE_ERROR_NO_SUCH_COLUMN)) {

                String getColumn = error.substring(error.indexOf(RotLiteConsts.ROTLITE_ERROR_NO_SUCH_COLUMN) + 1
                        + RotLiteConsts.ROTLITE_ERROR_NO_SUCH_COLUMN.length() + 1, error.length());

                getColumn = getColumn.substring(0, getColumn.indexOf(" "));
                //TODO: No Galaxy Trend, houve erro ao criar coluna porque o nome estava concatenado com ":". Entender melhor o porque
                getColumn = getColumn.replace(":", "").trim();

                if (foreignKeys.containsKey(getColumn)) {
                    Log.v("rotliteKey", getColumn + " faz parte de um relacionamento 2");
                }

                db.execSQL("ALTER TABLE " + name + " ADD COLUMN " + getColumn + "");
                saveLocal();

            }else{
                Log.e(TAG, "Caiu aqui: " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            throw e;
        }
        //content = null;

    }

    @Override
    public void beforeSaveWeb() {

    }

    @Override
    public void saveWeb(final RotLiteSyncUploadCallback callback) throws RotLiteException {

        configEndpoints();


        if (!endpointPut.equals("")) {
            if (method == RotLiteConsts.METHOD_INSERT || updateWebError == false) {
                method = RotLiteConsts.METHOD_UPDATE; //tenta atualizar primeiro
                Log.v(TAG, "update now! " + method + " - " + getId());
            } else {
                method = RotLiteConsts.METHOD_INSERT; //Se ele tentou atualizar agora vamos inserir!
                Log.v(TAG, "insert now! " + method + " - " + getId() + " " + jsonString());
            }
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {
                    saveData(jsonObjectToCloud(), new Callback() {
                        @Override
                        public void onFailure(Request request, IOException e) {
                            if (method == RotLiteConsts.METHOD_UPDATE && !endpointPut.equals("")) {
                                //Log.v("check-update", "Response: " + getId() + " " + e.getMessage());
                                //Log.v("check-update", "update! " + getId());
                                updateWebError = true;
                                try {
                                    saveWeb(callback);
                                } catch (RotLiteException e1) {
                                    e1.printStackTrace();
                                    Log.e(TAG, "Error on update " + getId() + " " + e1.getMessage());
                                    callback.onFailure(e1);
                                }
                            }else{
                                //Log.e("check-update", "Error on insert " + getId() + " " + e.getMessage());
                                callback.onFailure(new RotLiteException(e.getMessage()));
                            }
                        }

                        @Override
                        public void onResponse(Response response) throws IOException {

                            if (converter != null) {

                                //Log.v("check-update", "Usando converter no saveweb ");

                                if (response.isSuccessful()) {

                                    String responseString = response.body().string();

                                    if (!responseString.equals("")) {

                                        try {

                                            converter.setResponseString(responseString);

                                            Log.v(TAG, "response " + responseString);

                                            if (converter.getTypeObjectsDataReceived() == RotLiteConverter.JSON_ARRAY) {

                                                JsonArray data = converter.getObjects();

                                                if (data != null) {

                                                    Log.v(TAG, "DATA NOT NULL NO SAVE WEB");

                                            /*try {
                                                callback.done(generateObjects(data));
                                            } catch (RotLiteException e) {
                                                e.printStackTrace();
                                                callback.error(e);
                                            }*/

                                                } else {
                                                    JsonObject dataObj = converter.getObject();
                                                    Log.v(TAG, "DATAOBJ: " + dataObj.toString());
                                                    callback.onSuccess(response, generateObject(dataObj));
                                                    Log.v(TAG, "after onsync");
                                                    return;
                                                }

                                            } else {
                                                Log.e(TAG, "Tratamento de objeto recebido ainda não está pronto! Apenas array! Quando precisar bora fazer :D");
                                            }
                                        }catch(Exception e) {
                                            callback.onFailure(new RotLiteException(e.getMessage(), e.hashCode()));
                                            return;
                                        }

                                    }else{
                                        callback.onFailure(new RotLiteException("Response is empty"));
                                        return;
                                    }

                                }else{
                                    if (method == RotLiteConsts.METHOD_UPDATE && !endpointPut.equals("")) {
                                        Log.v(TAG, "Response 2: " + getId() + " " + response.body().string());
                                        updateWebError = true;
                                        try {
                                            saveWeb(callback);
                                        } catch (RotLiteException e1) {
                                            e1.printStackTrace();
                                            Log.e(TAG, "Error on update2 " + getId() + " " + e1.getMessage());
                                            callback.onFailure(e1);
                                            return;
                                        }
                                    }else{
                                        Log.e(TAG, "Error on insert 2 " + getId() + response.message());
                                        callback.onFailure(new RotLiteException(response.message()));
                                        return;
                                    }
                                }

                            } else {
                                if (response.isSuccessful()) {

                                    String responseStr = response.body().string();

                                    Log.v(TAG, "Resposta: " + responseStr);

                                    JsonParser jsonParser = new JsonParser();
                                    JsonObject data = jsonParser.parse(responseStr).getAsJsonObject();

                                    try {
                                        callback.onSuccess(response, generateObject(data));
                                        return;
                                    } catch (RotLiteException e) {
                                        e.printStackTrace();
                                        callback.onFailure(new RotLiteException(e.getMessage(), e.hashCode()));
                                        return;
                                    }

                                } else {
                                    String message = response.body().string();
                                    if (message.equals("")) {
                                        message = response.code() + " " + response.message();
                                    }
                                    callback.onFailure(new RotLiteException(message, response.code()));
                                    return;
                                }
                            }
                        }

                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onFailure(new RotLiteException(e.getMessage()));
                    return;
                }

            }
        };
        new Thread(runnable).start();

    }

    /*@Override
    public void saveWeb(final Callback callback) throws Exception {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {
                    saveData(jsonObjectToCloud(), callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onFailure(null, new IOException(e.getMessage()));
                }

            }
        };
        new Thread(runnable).start();

    }*/

    @Override
    public void save(final RotLiteSyncCallback callback) throws Exception {

        //TODO: Pensa no caso do SaveLocal falhar... O que vai acontecer ao salvar o RotLiteModel temp
        this.saveLocal(); //Primeiro salvamos os dados no local

        /**
         * Agora salvamos os dados numa tabela temporária, essa tabela possuirá
         * todos os dados que ainda não foram enviadas para a web.
         */

        final String classInstanceName = this.classInstance.getName();

        final RotLiteModel temp = new RotLiteModel(context);
        temp.put("model", classInstanceName);
        temp.put("data_id", _id);
        try {
            temp.saveLocal();
            callback.onSuccessLocal((T) RotLiteObject.this);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFailureLocal(new RotLiteException(e.getMessage(), e.hashCode()));
        }

        this.beforeSaveWeb();

        saveWeb(new RotLiteSyncUploadCallback() {
            @Override
            public void onFailure(RotLiteException e) {
                callback.onFailureCloud(e);
            }

            @Override
            public void onSuccess(Response response, RotLiteObject obj) {

                RotLiteModel temp = new RotLiteModel(context);
                temp.where("model = '" + classInstanceName + "' and data_id = '" + _id + "'");
                if (temp.delete()) {
                    Log.v("deleted", "2true " + jsonString());
                }else{
                    Log.v("deleted", "2false " + jsonString());
                }

                callback.onSuccessCloud(response, obj);
                setIsSync(true);
                update();

            }
        });

    }

    @Override
    public boolean tbExists(String tbname) {

        if (db == null || !db.isOpen()) {
            db = RotLite.getInstance().getDataBase(context);
        }

        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tbname + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    @Override
    public void createTable(String tbname) throws Exception {

        Set<String> keys = content.keySet();

        String keyString = "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + DEFAULT_ID_FIELD_NAME + " TEXT, rotlite_is_sync LONG,";
        String foreignKeyString = "";

        for (String key : keys) {

            try {

                if (!key.equals("_id") && !key.equals(DEFAULT_ID_FIELD_NAME) && !key.equals(ROTLITE_IS_SYNC)) {
                    String type = dataType.get(key);

                    if (type.equals(RotLiteConsts.DATA_TYPE_STRING)) {
                        keyString = keyString + " " + key + " TEXT,";
                    } else if (type.equals(RotLiteConsts.DATA_TYPE_INTEGER)) {
                        keyString = keyString + " " + key + " INTEGER,";
                    } else if (type.equals(RotLiteConsts.DATA_TYPE_DOUBLE)) {
                        keyString = keyString + " " + key + " DOUBLE,";
                    } else if (type.equals(RotLiteConsts.DATA_TYPE_LONG)) {
                        keyString = keyString + " " + key + " INTEGER,";
                    } else if (type.equals(RotLiteConsts.DATA_TYPE_BOOLEAN)) {
                        keyString = keyString + " " + key + " TINYINT,";
                    } else if (type.equals(RotLiteConsts.DATA_TYPE_DATE)) {
                        keyString = keyString + " " + key + " DATE,";
                    } else if (type.equals(RotLiteConsts.DATA_TYPE_DATETIME)) {
                        keyString = keyString + " " + key + " DATETIME,";
                    }

                    if (foreignKeys.containsKey(key)) {

                        ForeignKeyContent fk = foreignKeys.get(key);
                        foreignKeyString = foreignKeyString + ", FOREIGN KEY(" + key + ") REFERENCES " + fk.getTableName() + "(" + fk.getReferences() + ")";

                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Get value error: " + e.getMessage());
            }

        }

        keyString = "CREATE TABLE " + tbname + " (" + keyString.substring(0, keyString.length() - 1) + foreignKeyString + ", UNIQUE(" + DEFAULT_ID_FIELD_NAME + "))";

        try {

            db.execSQL(keyString);
            Log.v(TAG, "Created table: " + keyString);

        }catch (SQLiteException e) {

            String error = e.getMessage();

            if (foreignKeys.size() > 0) {
                Log.e(TAG, "Erro ao criar tabela " + error);
            }

            throw new Exception("Error on create table " + e.getMessage());

        }

        /*if (tbExists(tbname)) {

            return true;
        } else {
            Log.e(TAG, "Error on create table " + name);
            return false;
        }*/

    }

    @Override
    public void where(String where) {
        this.where = where;
    }

    @Override
    public List<T> find() throws RotLiteException {

        final List<T> list = new ArrayList<>();

        String sql = "SELECT * FROM ";
        String sqlTableNames = "";
        String aliases = "";

        if (include.size() > 0) {

            //sql = sql + name + ".* AS " + name + "_cm.*,";
            sqlTableNames = sqlTableNames + name + ",";
            String join = "";

            for (int i = 0; i < include.size(); i++) {

                String column = include.get(i);

                ForeignKeyContent fk = foreignKeys.get(column);

                sqlTableNames = sqlTableNames + fk.getTableName() + ",";
                join = join + fk.getTableName() + " ON " + name + "." + column + " = " + fk.getTableName() + "." + fk.references + " OR";

            }

            sqlTableNames = sqlTableNames.substring(0, sqlTableNames.length() - 1);

            try {

                join = join.substring(0, join.length() - 2);
                sql = sql + sqlTableNames;

                Map<String, List<String>> tablesColumns = new HashMap<>();

                Cursor getColumnsCursor = db.rawQuery(sql + " LIMIT 1", null);
                if (getColumnsCursor.moveToFirst()) {
                    do {
                        String[] cols = getColumnsCursor.getColumnNames();
                        tablesColumns = getTablesColumns(cols);
                    }while(getColumnsCursor.moveToNext());

                    for (Map.Entry<String, List<String>> entry : tablesColumns.entrySet()) {

                        String table = entry.getKey();
                        List<String> columns = entry.getValue();

                        for (String col : columns) {

                            aliases = aliases + table + "." + col + " AS `" + table + "_rotlite_column_" + col + "`,";

                        }

                    }

                }

                aliases = aliases.substring(0, aliases.length() - 1);

                sql = "SELECT " + aliases + " FROM " + name + " LEFT JOIN " + join + " WHERE " + where;

                //sql = "SELECT clientes._id AS `clientes__id`,clientes.uuid AS `clientes_uuid`,clientes.rotlite_is_sync AS `clientes_rotlite_is_sync`,clientes.categoria AS `clientes_categoria`,clientes.nome AS `clientes_nome`,clientes.updatedAt AS `clientes_updatedAt`,clientes.createdAt AS `clientes_createdAt`,categorias._id AS `categorias__id`,categorias.cat_id AS `categorias_cat_id`,categorias.rotlite_is_sync AS `categorias_rotlite_is_sync`,categorias.cat_nome AS `categorias_cat_nome`,categorias.updatedAt AS `categorias_updatedAt`,categorias.createdAt AS `categorias_createdAt` FROM clientes LEFT JOIN categorias ON clientes.clientes_categoria = categorias.categorias_cat_id  WHERE 1 = 1";

            }catch (Exception e) {
                throw e;
            }

        }else{

            sql = sql + name + " WHERE " + where;

        }

        //String sql = "SELECT * FROM " + name + " WHERE " + where;

        if (order != null && !order.equals("")) {
            sql = sql + order;
        }

        if (limit != null && !limit.equals("")) {
            sql = sql + limit;
        }

        if (group != null && !group.equals("")) {
            sql = sql + group;
        }

        query = sql;

        if (this.from == RotLiteConsts.FROM_LOCAL) {

            try {

                final Cursor cursor = db.rawQuery(query, null);
                return findExecute(cursor, list, null);

            } catch (SQLiteDatabaseCorruptException e) {
                throw new RotLiteException("SQLiteDatabaseCorruptException " + e.getMessage() + "", e.hashCode());
            } catch (android.database.sqlite.SQLiteConstraintException e) {
                throw new RotLiteException("SQLiteConstraintException " + e.getMessage() + "", e.hashCode());
            } catch (SQLiteException e) {
                String error = e.getMessage();

                if (error.contains(RotLiteConsts.ROTLITE_ERROR_NO_SUCH_TABLE)) {
                    return new ArrayList<>(); //Se a tabela não existir, não há registros, então retorna uma lista vazia!
                } else {

                    throw new RotLiteException("SQLiteException " + e.getMessage() + "", e.hashCode());
                }
            } catch (Exception e) {
                throw new RotLiteException("Exception::: " + e.getMessage() + "", e.hashCode());
            }
        } else if (this.from == RotLiteConsts.FROM_WEB) {

            try {

                Log.v(TAG, "estamos aqui");
                return findFromWebMethod(null, list);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RotLiteException(e.getMessage(), e.hashCode());
            }

        } else {
            Log.v(TAG, "vish ta null véi");
            return null;
        }

    }

    private List<T> findFromWebMethod(final RotLiteCallback callback, final List<T> list) throws IOException {
        currentPage = 1;
        return findFromWebMethod(callback, list, null);
    }



    private List<T> findFromWebMethod(final RotLiteCallback callback, final List<T> list, final List loadedModels) throws IOException {

        if (callback != null) {

            getData(jsonObjectToCloud(), new Callback() {

                @Override
                public void onFailure(Request request, IOException e) {
                    callback.error(new RotLiteException("Failure request: " + e.getMessage()));
                    return;
                }

                @Override
                public void onResponse(Response response) throws IOException {

                    //Não curte o padrão de recebimento de dados da RotLite? Ok, aceitamos o seu formato também!
                    if (converter != null) {

                        //TODO: MADRUGA Inicia DAQUI
                        //Gson gson = new Gson();

                        if (response.isSuccessful()) {

                            final JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), "UTF-8"));
                            converter.setJsonReader(reader);

                            converter.getJsonReader(new RotLiteConverter.DataObjects() {
                                @Override
                                public void getData(JsonReader data) {
                                    List models = generateObjectsJsonReader(reader);
                                    if (loadedModels != null) {
                                        models.addAll(loadedModels);
                                    }

                                    int count = models.size();
                                    if (table.perPageCount() != 0 && count == table.perPageCount()) {
                                        currentPage++;
                                        try {
                                            findFromWebMethod(callback, list, models);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Log.e(TAG, e.getMessage());
                                        }
                                    } else {
                                        currentPage = 1;
                                        if (count >= 0) {
                                            callback.done(models);
                                            return;
                                        }
                                    }
                                }
                            });
                            reader.close();

                        }else{

                            callback.error(new RotLiteException(response.message(), response.code()));
                            return;

                        }

                        /*

                        JsonToken token = reader.peek();

                        //Se recebermos um objeto do backend...
                        if (token == JsonToken.BEGIN_OBJECT) {

                            List modelsFromWeb = new ArrayList();

                            reader.beginObject();
                            while (reader.hasNext()) {
                                String name = reader.nextName();

                                Log.v(TAG1, "name: " + name);



                                if (name.equals("data")) {

                                    modelsFromWeb = generateObjectsJsonReader(reader);
                                    if (loadedModels != null)
                                        modelsFromWeb.addAll(loadedModels);

                                    int count = modelsFromWeb.size();

                                    if (table.perPageCount() != 0 && count == table.perPageCount()) {
                                        currentPage++;
                                        findFromWebMethod(callback, list, modelsFromWeb);
                                    } else {
                                        currentPage = 1;
                                        if (count >= 0) {
                                            callback.done(modelsFromWeb);
                                            return;
                                        }
                                    }

                                }else{
                                    reader.skipValue();
                                }
                            }

                            modelsFromWeb.clear();

                            reader.endObject();
                            reader.close();

                        }else{

                            //Se recebermos um array do backend...

                            List modelsFromWeb = generateObjectsJsonReader(reader);

                            if (loadedModels != null)
                                modelsFromWeb.addAll(loadedModels);

                            int count = modelsFromWeb.size();

                            if (table.perPageCount() != 0 && count == table.perPageCount()) {
                                currentPage++;
                                findFromWebMethod(callback, list, modelsFromWeb);
                            } else {
                                currentPage = 1;
                                if (count >= 0) {
                                    callback.done(modelsFromWeb);
                                    return;
                                }
                            }

                            reader.close();

                        }*/

                        /*String responseString = response.body().string();

                        Log.v(TAG, "Usando converter ");
                        Log.v(TAG, "Response: " + responseString);

                        if (!responseString.equals("")) {

                            try {

                                if (response.isSuccessful()) {

                                    converter.setResponseString(responseString);

                                    if (converter.getTypeObjectsDataReceived() == RotLiteConverter.JSON_ARRAY) {

                                        JsonArray data = converter.getObjects();
                                        Log.v("rotLiteLinspector", "Boatos que nessa array há " + data.size() + " dados");

                                        int count = data.size();
                                        Log.d("MADMAX", String.format("perPageCount: %d", table.perPageCount()));
                                        Log.d("MADMAX", String.format("Count object: %d", converter.getObjects().size()));

                                        List downloadedModels;
                                        try {
                                            downloadedModels = generateObjects(data);
                                        } catch (RotLiteException e) {
                                            e.printStackTrace();
                                            callback.error(e);
                                            return;
                                        }

                                        if (loadedModels != null)
                                            downloadedModels.addAll(loadedModels);

                                        if (table.perPageCount() != 0 && count == table.perPageCount()) {
                                            currentPage++;
                                            Log.d("MADMAX", String.format("Here we go again with %d pois already page %d", downloadedModels.size(), currentPage));
                                            findFromWebMethod(callback, list, downloadedModels);
                                        } else {
                                            currentPage = 1;
                                            Log.d("MADMAX", String.format("Finish"));
                                            if (data != null) {
                                                callback.done(downloadedModels);
                                                return;
                                            }
                                        }

                                    } else {

                                        Log.e(TAG, "Tratamento de objeto recebido ainda não está pronto! Apenas array! Quando precisar bora fazer :D");

                                    }

                                } else {
                                    String message = "";
                                    try {
                                        message = response.body().string();
                                        if (message.equals("")) {
                                            message = response.code() + " " + response.message();
                                        }
                                        callback.error(new RotLiteException(message, response.code()));
                                    } catch (Exception e) {
                                        callback.error(new RotLiteException(response.message(), response.code()));
                                    }

                                    return;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                callback.error(new RotLiteException(e.getMessage(), e.hashCode()));
                                Log.e("RotLiteSync", "Error 1: " + e.getMessage() + " - " + response.body().contentLength());
                                return;
                            }

                        } else {


                        }*/

                    } else {

                        if (response.isSuccessful()) {

                            JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), "UTF-8"));

                            JsonToken token = reader.peek();

                            //Se recebermos um array do backend...

                            List modelsFromWeb = generateObjectsJsonReader(reader);
                            Log.v(TAG1, "Total de models: " + modelsFromWeb.size());

                            if (loadedModels != null)
                                modelsFromWeb.addAll(loadedModels);

                            int count = modelsFromWeb.size();

                            if (table.perPageCount() != 0 && count == table.perPageCount()) {
                                currentPage++;
                                findFromWebMethod(callback, list, modelsFromWeb);
                            } else {
                                currentPage = 1;
                                if (count >= 0) {
                                    callback.done(modelsFromWeb);
                                    return;
                                }
                            }

                            reader.close();

                            /*String responseStr = response.body().string();

                            JsonParser jsonParser = new JsonParser();

                            try {

                                JsonArray data = jsonParser.parse(responseStr).getAsJsonArray();

                                callback.done(generateObjects(data));
                                return;

                            } catch (Exception e) {

                                JsonObject data = jsonParser.parse(responseStr).getAsJsonObject();

                                T obj;
                                try {
                                    obj = generateObject(data);

                                    if (obj != null) list.add(obj);

                                    callback.done(list);
                                    return;

                                } catch (RotLiteException e1) {
                                    e1.printStackTrace();
                                    callback.error(new RotLiteException(e1.getMessage(), response.code()));
                                }

                            }*/

                        } else {

                            callback.error(new RotLiteException(response.message(), response.code()));
                            return;
                        }
                    }
                }

            });
        } else {
            Call data = getData(jsonObjectToCloud(), null);
            Response response = data.execute();
            if (response.isSuccessful()) {

                String responseStr = response.body().string();

                JsonParser jsonParser = new JsonParser();

                try {

                    JsonArray arrayData = jsonParser.parse(responseStr).getAsJsonArray();
                    Log.v(TAG, "Array size: " + arrayData.size());

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            } else {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    //TODO: MADRUGA
    public List generateObjectsJsonReader(JsonReader reader) {

        List<T> list = new ArrayList<>();

        try {

            Log.v(TAG1, "Token Reader: " + reader.peek().name());
            reader.beginArray();
            while(reader.hasNext()) {
                if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                    try {
                        list.add(generateObjectJsonReader(reader));
                    } catch (RotLiteException e) {
                        e.printStackTrace();
                        Log.e(TAG1, "Error on generate object from json reader: " + e.getMessage());
                    }
                }
            }
            reader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG1, "Error on read array " + e.getMessage());
        }

        return list;

    }

    public List generateObjects(JsonArray data) throws RotLiteException {
        List<T> list = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {

            try {

                T obj;
                try {

                    obj = classInstance.getDeclaredConstructor(Context.class).newInstance(context);

                    JsonObject object = data.get(i).getAsJsonObject();

                    if (object.get(DEFAULT_ID_FIELD_NAME) != null) {

                        obj.setId(object.get(DEFAULT_ID_FIELD_NAME).getAsString());
                        if (object.get("_id") != null) obj.setLocalId(object.get("_id").getAsInt());
                        obj.setTbName(name);

                        if (converter != null) {
                            obj.setConverter(converter);
                        }

                        defaultJSONToModel(obj, object);

                        /*boolean hasCustomJSONToModel = obj instanceof RotLiteCustomJsonToModel;
                        Log.d("CustomJSON",String.format("Object implements interface? %b", hasCustomJSONToModel));
                        if (hasCustomJSONToModel){
                            ((RotLiteCustomJsonToModel) obj).fromJson(object);
                        }else {
                            defaultJSONToModel(obj, object);
                        }*/
                    } else {
                        obj = null;
                    }

                    if (obj != null) list.add(obj);

                } catch (InstantiationException e) {
                    Log.e(TAG, "InstantiationException: " + e.getMessage());
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "IllegalAccessException: " + e.getMessage());
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "NoSuchMethodException: " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    Log.e(TAG, "InvocationTargetException: " + e.getMessage());
                    e.printStackTrace();
                }

            } catch (IllegalStateException e) {
                Log.e(TAG, e.getMessage());
            }

        }

        return list;
    }

    /*
    * Converte um Elemento JSON do Array padrão de resposta REST em um RotLiteModel
    *
    * Esse método "default", usa as chaves do JSON como campos da tabela, e os valores das colunas
    * extraidos do valor da chave JSON
    *
    * */
    protected void defaultJSONToModel(T obj, JsonObject object) throws RotLiteException {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {

            /*if (activityLogs)
                Log.v(TAG, entry.getKey() + ": " + entry.getValue());*/

            String type = entry.getValue().getClass().toString();

                try {
                    if (type.contains("JsonArray")) {
                        try {
                            obj.put(entry.getKey(), new JSONArray(entry.getValue().getAsJsonArray().toString()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if (type.contains("JsonObject")) {
                        try {
                            obj.put(entry.getKey(), new JSONObject(entry.getValue().getAsJsonObject().toString()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else {
                        obj.put(entry.getKey(), entry.getValue().getAsString());
                    }
                } catch (Exception e1) {
                    try {
                        obj.put(entry.getKey(), entry.getValue().getAsInt());
                    } catch (Exception e2) {
                        try {
                            obj.put(entry.getKey(), entry.getValue().getAsDouble());
                        } catch (Exception e3) {
                            try {
                                obj.put(entry.getKey(), entry.getValue().getAsLong());
                            } catch (Exception e4) {
                                try {
                                    obj.put(entry.getKey(), entry.getValue().getAsBoolean());
                                } catch (Exception e5) {
                                    try {
                                        obj.put(entry.getKey(), entry.getValue().getAsJsonObject().toString());
                                    } catch (Exception e6) {
                                        try {
                                            obj.put(entry.getKey(), entry.getValue().getAsJsonArray().toString());
                                        } catch (Exception e7) {
                                            try {
                                                entry.getValue().getAsJsonNull();
                                            } catch (Exception e8) {
                                                throw new RotLiteException("Dado não suportado: " + entry.getKey() + " " + e8.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

    }

    public T generateObject(JsonObject data) throws RotLiteException {

        T obj = null;
        try {
            obj = classInstance.getDeclaredConstructor(Context.class).newInstance(context);

            JsonObject object = data;

            if (object.get(DEFAULT_ID_FIELD_NAME) != null) {

                obj.setId(object.get(DEFAULT_ID_FIELD_NAME).getAsString());
                if (object.get("_id") != null)
                    obj.setLocalId(object.get("_id").getAsInt());
                obj.setTbName(name);

                defaultJSONToModel(obj, object);
            } else {
                obj = null;
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

        return obj;

    }

    //TODO: MADRUGA
    public T generateObjectJsonReader(JsonReader reader) throws RotLiteException {

        T obj = null;
        try {
            obj = classInstance.getDeclaredConstructor(Context.class).newInstance(context);

            try {

                reader.beginObject();

                //Log.e(TAG1, "object ");
                while(reader.hasNext()) {

                    //Log.v(TAG1, "Name: " + reader.peek().name());

                    String name = "";

                    //Log.v(TAG1, "Get Peek: " + reader.peek());

                    if (reader.peek() == JsonToken.NAME)
                        name = reader.nextName();

                    if (reader.peek() != JsonToken.NULL) {
                        //Log.v(TAG1, "Peek: " + reader.peek().name());

                        if (reader.peek() == JsonToken.STRING) {
                            //Log.v(TAG1, "'" + name + "' String: " + reader.nextString());
                            obj.put(name, reader.nextString());
                        }

                        if (reader.peek() == JsonToken.BOOLEAN) {
                            //Log.v(TAG1, "'" + name + "' Boolean: " + reader.nextBoolean());
                            obj.put(name, reader.nextBoolean());
                        }

                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            //Log.v(TAG1, "'" + name + "' Object");
                            obj.put(name, getJSONObjectFromJsonReader(reader));
                        }

                        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                            obj.put(name, getJSONArrayFromJsonReader(reader));
                        }

                        try {
                            if (reader.peek() == JsonToken.NUMBER) {
                                //Log.v(TAG1, "'" + name + "' Number: " + reader.nextInt());
                                obj.put(name, reader.nextInt());
                            }
                        }catch(NumberFormatException e) {
                            if (reader.peek() == JsonToken.NUMBER) {
                                //Log.v(TAG1, "'" + name + "' Double: " + reader.nextDouble());
                                obj.put(name, reader.nextDouble());
                            }
                        }

                    }else{
                        reader.skipValue();
                    }

                }
                //Log.e(TAG1, "end===");

                reader.endObject();

                /*if (object.get(DEFAULT_ID_FIELD_NAME) != null) {

                    obj.setId(object.get(DEFAULT_ID_FIELD_NAME).getAsString());
                    if (object.get("_id") != null)
                        obj.setLocalId(object.get("_id").getAsInt());
                    obj.setTbName(name);

                    defaultJSONToModel(obj, object);
                } else {
                    obj = null;
                }*/

            } catch (IOException e) {
                e.printStackTrace();
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

        return obj;

    }

    //TODO: MADRUGA
    public static JSONObject getJSONObjectFromJsonReader(JsonReader reader) {

        final JSONObject obj = new JSONObject();

        try {
            reader.beginObject();
            while(reader.hasNext()) {

                try {

                    String name = "";

                    //Log.v(TAG1, "Get Peek: " + reader.peek());

                    if (reader.peek() == JsonToken.NAME)
                        name = reader.nextName();

                    if (reader.peek() != JsonToken.NULL) {

                        if (reader.peek() == JsonToken.STRING) {
                            obj.put(name, reader.nextString());
                        }

                        if (reader.peek() == JsonToken.BOOLEAN) {
                            obj.put(name, reader.nextBoolean());
                        }

                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            obj.put(name, getJSONObjectFromJsonReader(reader));
                        }

                        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                            reader.skipValue();
                        }

                        try {
                            if (reader.peek() == JsonToken.NUMBER) {
                                obj.put(name, reader.nextInt());
                            }
                        }catch(NumberFormatException e) {
                            if (reader.peek() == JsonToken.NUMBER) {
                                obj.put(name, reader.nextDouble());
                            }
                        }

                    }else{
                        reader.skipValue();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return obj;

    }

    //TODO: MADRUGA
    public static JSONArray getJSONArrayFromJsonReader(JsonReader reader) {

        final JSONArray obj = new JSONArray();

        try {
            reader.beginArray();
            while(reader.hasNext()) {

                try {

                    String name = "";

                    //Log.v(TAG1, "Get Peek: " + reader.peek());

                    if (reader.peek() == JsonToken.NAME)
                        name = reader.nextName();

                    if (reader.peek() != JsonToken.NULL) {

                        if (reader.peek() == JsonToken.STRING) {
                            obj.put(reader.nextString());
                        }

                        if (reader.peek() == JsonToken.BOOLEAN) {
                            obj.put(reader.nextBoolean());
                        }

                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            obj.put(getJSONObjectFromJsonReader(reader));
                        }

                        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                            obj.put(getJSONArrayFromJsonReader(reader));
                        }

                        try {
                            if (reader.peek() == JsonToken.NUMBER) {
                                obj.put(reader.nextInt());
                            }
                        }catch(NumberFormatException e) {
                            if (reader.peek() == JsonToken.NUMBER) {
                                obj.put(reader.nextDouble());
                            }
                        }

                    }else{
                        reader.skipValue();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return obj;

    }

    @Override
    public void findInBackground(final RotLiteCallback callback) {

        final List<T> list = new ArrayList<>();

        String sql = "SELECT * FROM ";
        String sqlTableNames = "";
        String aliases = "";

        if (include.size() > 0) {

            //sql = sql + name + ".* AS " + name + "_cm.*,";
            sqlTableNames = sqlTableNames + name + ",";
            String join = "";

            for (int i = 0; i < include.size(); i++) {

                String column = include.get(i);

                ForeignKeyContent fk = foreignKeys.get(column);

                sqlTableNames = sqlTableNames + fk.getTableName() + ",";
                join = join + fk.getTableName() + " ON " + name + "." + column + " = " + fk.getTableName() + "." + fk.references + " OR";

            }

            sqlTableNames = sqlTableNames.substring(0, sqlTableNames.length() - 1);

            try {

                join = join.substring(0, join.length() - 2);
                sql = sql + sqlTableNames;

                Map<String, List<String>> tablesColumns = new HashMap<>();

                Cursor getColumnsCursor = db.rawQuery(sql + " LIMIT 1", null);
                if (getColumnsCursor.moveToFirst()) {
                    do {
                        String[] cols = getColumnsCursor.getColumnNames();
                        tablesColumns = getTablesColumns(cols);
                    }while(getColumnsCursor.moveToNext());

                    for (Map.Entry<String, List<String>> entry : tablesColumns.entrySet()) {

                        String table = entry.getKey();
                        List<String> columns = entry.getValue();

                        for (String col : columns) {

                            aliases = aliases + table + "." + col + " AS `" + table + "_rotlite_column_" + col + "`,";

                        }

                    }

                }

                aliases = aliases.substring(0, aliases.length() - 1);

                sql = "SELECT " + aliases + " FROM " + name + " LEFT JOIN " + join + " WHERE " + where;

                //sql = "SELECT clientes._id AS `clientes__id`,clientes.uuid AS `clientes_uuid`,clientes.rotlite_is_sync AS `clientes_rotlite_is_sync`,clientes.categoria AS `clientes_categoria`,clientes.nome AS `clientes_nome`,clientes.updatedAt AS `clientes_updatedAt`,clientes.createdAt AS `clientes_createdAt`,categorias._id AS `categorias__id`,categorias.cat_id AS `categorias_cat_id`,categorias.rotlite_is_sync AS `categorias_rotlite_is_sync`,categorias.cat_nome AS `categorias_cat_nome`,categorias.updatedAt AS `categorias_updatedAt`,categorias.createdAt AS `categorias_createdAt` FROM clientes LEFT JOIN categorias ON clientes.clientes_categoria = categorias.categorias_cat_id  WHERE 1 = 1";

            }catch (Exception e) {
                callback.error(new RotLiteException(e.getMessage(), e.hashCode()));
            }

        }else{

            sql = sql + name + " WHERE " + where;

        }

        if (order != null && !order.equals("")) {
            sql = sql + order;
        }

        if (limit != null && !limit.equals("")) {
            sql = sql + limit;
        }

        if (group != null && !group.equals("")) {
            sql = sql + group;
        }

        //Log.v(TAG, "SQL (2): " + sql);

        query = sql;

        if (this.from == RotLiteConsts.FROM_LOCAL) {

            try {

                final Cursor cursor = db.rawQuery(query, null);

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        try {
                            findExecute(cursor, list, callback);
                        } catch (RotLiteException e) {
                            e.printStackTrace();
                            Log.e(TAG, e.getMessage());
                        }

                    }
                };
                new Thread(runnable).start();

            } catch (SQLiteDatabaseCorruptException e) {
                callback.error(new RotLiteException("SQLiteDatabaseCorruptException " + e.getMessage().toString() + "", e.hashCode()));
            } catch (android.database.sqlite.SQLiteConstraintException e) {
                callback.error(new RotLiteException("SQLiteConstraintException " + e.getMessage(), e.hashCode()));
            } catch (SQLiteException e) {
                String error = e.getMessage();
                e.printStackTrace();

                if (error.contains(RotLiteConsts.ROTLITE_ERROR_NO_SUCH_TABLE)) {
                    callback.done(new ArrayList<>()); //Se a tabela não existir, não há registros, então retorna uma lista vazia!
                } else {
                    callback.error(new RotLiteException("SQLiteException.. " + e.getMessage(), e.hashCode()));
                }

            } catch (Exception e) {
                callback.error(new RotLiteException("Exception " + e.getMessage(), e.hashCode()));
            }
        } else if (this.from == RotLiteConsts.FROM_WEB) {

            try {

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        try {
                            findFromWebMethod(callback, list);
                        } catch (IOException e) {
                            e.printStackTrace();
                            callback.error(new RotLiteException(e.getMessage(), e.hashCode()));
                        }

                    }
                };
                new Thread(runnable).start();

            } catch (Exception e) {
                e.printStackTrace();
                callback.error(new RotLiteException(e.getMessage(), e.hashCode()));
            }

        }

    }

    private Map<String, List<String>> getTablesColumns(String[] cols) {
        Map<String, List<String>> tablesColumns = new HashMap<>();
        List columnsName = new ArrayList();

        String currentTable = name;
        List tables = new ArrayList();

        //Log.v(TAG, "START =============== " + cols.length);
        for (int z = 0; z < cols.length; z++) {

            String col = cols[z].replace(currentTable + "_rotlite_column_", "");

            if (include.size() > 0) {
                if (col.equals("_id") || col.contains("rotlite_column__id")) {
                    columnsName = new ArrayList();
                    if (tables.size() > 0) {

                        int getNextTablePos = tables.size() - 1;
                        int i = 0;

                        for (Map.Entry<String, ForeignKeyContent> entry : foreignKeys.entrySet()) {
                            if (i == getNextTablePos) {
                                ForeignKeyContent fk = entry.getValue();
                                currentTable = fk.getTableName();
                                //Log.v(TAG, "CurrentTable: " + currentTable + "; Pos: " + z);
                            }
                            i++;
                        }

                    }else{
                        //Log.e(TAG, "Tables Size is 0 " + currentTable);
                    }

                    //Log.v(TAG, "--> new table (" + currentTable + ")");
                    columnsName.add(col.replace(currentTable + "_rotlite_column_", ""));
                    tablesColumns.put(currentTable, columnsName);
                    tables.add(currentTable);
                    //Log.v(TAG, "Tables size: " + tables.size());

                }else{
                    if (tablesColumns.containsKey(currentTable)) {

                        List tbColumns = tablesColumns.get(currentTable);
                        tbColumns.add(col.replace(currentTable + "_rotlite_column_", ""));
                        //Log.v(TAG, "COLUMN " + col.replace(currentTable + "_rotlite_column_", "") + " TABLE " + currentTable + " TOTAL_COLUMNS " + tbColumns.size());

                    }
                }
            }

        }
        //Log.v(TAG, "END ================");
        return tablesColumns;
    }

    private List<T> findExecute(Cursor cursor, List<T> list, RotLiteCallback callback) throws RotLiteException {

        int pos = 0;
        if (cursor.moveToFirst()) {

            do {

                /**
                 * Observações: Quando houver ForeignKeys, as primeiras colunas até o segundo "_id"
                 * pertencem à tabela principal, as seguintes pertencerão âs tabelas relacionadas,
                 * sendo que cada "_id" é o indicador de que as próximas colunas pertencem â outras tabelas.
                 *
                 * Por exemplo, se executarmos o Log abaixo com foreign key em execução, notaremos
                 * o nome das colunas de todas as tabelas relacionadas e este é o método de identificar
                 * a quais tabelas pertencem estas colunas.
                 */

                String[] cols = cursor.getColumnNames();

                if (include.size() > 0) {

                    Log.v(TAG, "start here;");

                    //String = nome da tabela; List = colunas da tabela
                    Map<String, List<String>> tablesColumns = getTablesColumns(cols);

                    T obj = null;

                    for (Map.Entry<String, List<String>> entry : tablesColumns.entrySet()) {

                        String table = entry.getKey();
                        List<String> columns = entry.getValue();

                        try {

                            for (Map.Entry<String, ForeignKeyContent> entry2 : foreignKeys.entrySet()) {
                                ForeignKeyContent fk = entry2.getValue();
                                String tb = fk.getTableName();
                                if (tb.equals(table) && !table.equals(name)) {
                                    obj = (T) fk.getModel();
                                    Log.v(TAG, "OPA " + table);
                                }else if (table.equals(name)){
                                    obj = classInstance.getDeclaredConstructor(Context.class).newInstance(context);
                                }
                                //Log.v(TAG, "CurrentTable: " + currentTable + "; Pos: " + z);

                            }

                            Log.v(TAG, "--> table before: " + table + "; " + obj.getTbName());

                            obj.setTbName(table);

                            Log.v(TAG, "--> table after: " + table + "; " + obj.getTbName());

                            for (int i = 0; i < columns.size(); i++) {

                                String column = columns.get(i);
                                String colFormat = table + "_rotlite_column_" + column;
                                Log.v(TAG, "column: " + colFormat + "; index: " + cursor.getColumnIndex(colFormat) + "; str: " + columns.get(i) + "; value: " + cursor.getString(cursor.getColumnIndex(colFormat)));

                                //obj.setId(cursor.getString(1));
                                if (column.equals("_id")) obj.setLocalId(cursor.getInt(cursor.getColumnIndex(colFormat)));

                                switch (cursor.getType(i)) {
                                    case Cursor.FIELD_TYPE_NULL:
                                        obj.putNull(colFormat);
                                        break;
                                    case Cursor.FIELD_TYPE_BLOB:
                                        obj.put(column, cursor.getString(cursor.getColumnIndex(colFormat)));
                                        break;
                                    case Cursor.FIELD_TYPE_FLOAT:
                                        obj.put(column, cursor.getFloat(cursor.getColumnIndex(colFormat)));
                                        break;
                                    case Cursor.FIELD_TYPE_INTEGER:
                                        obj.put(column, cursor.getInt(cursor.getColumnIndex(colFormat)));
                                        break;
                                    case Cursor.FIELD_TYPE_STRING:
                                        String str = cursor.getString(cursor.getColumnIndex(colFormat));
                                        if (isValidJSONArray(str)) {
                                            try {
                                                obj.put(column, new JSONArray(str));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        } else if (isValidJSONObject(str)) {
                                            try {
                                                obj.put(column, new JSONObject(str));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            obj.put(column, cursor.getString(cursor.getColumnIndex(colFormat)));
                                        }
                                        break;
                                }
                            }

                            if (table.equals(name)) { list.add(obj); } else {

                                for (Map.Entry<String, ForeignKeyContent> entry2 : foreignKeys.entrySet()) {
                                    ForeignKeyContent fk = entry2.getValue();
                                    String tb = fk.getTableName();
                                    if (tb.equals(table)) {
                                        Log.v(TAG, "set: " + fk.getColumn() + "; " + obj.jsonString());
                                        Log.v(TAG, "local id " + obj.getLocalId());
                                        list.get(pos).includedModels.put(fk.getColumn(), obj);
                                    }
                                    //Log.v(TAG, "CurrentTable: " + currentTable + "; Pos: " + z);

                                }

                            }

                        } catch (InstantiationException e) {
                            Log.e(TAG, "InstantiationException: " + e.getMessage());
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, "IllegalAccessException: " + e.getMessage());
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            Log.e(TAG, "NoSuchMethodException: " + e.getMessage());
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            Log.e(TAG, "InvocationTargetException: " + e.getMessage());
                            e.printStackTrace();
                        }

                        /*for (String col : columns) {

                            Log.v(TAG, "col: " + col + "; table: " + table);

                        }*/

                    }

                    Log.v(TAG, "end here;");

                }else{

                    T obj;
                    try {
                        obj = classInstance.getDeclaredConstructor(Context.class).newInstance(context);

                        obj.setId(cursor.getString(1));
                        obj.setLocalId(cursor.getInt(0));
                        obj.setTbName(name);

                        //Log.v(TAG, "START COLUMN NAME ===============");

                        for (int i = 0; i < cols.length; i++) {

                            String column = cols[i].replace(name + "_rotlite_column_", "");

                            //Log.v(TAG, "COlUMN NAME: " + cursor.getColumnName(i));

                            switch (cursor.getType(i)) {
                                case Cursor.FIELD_TYPE_NULL:
                                    obj.putNull(column);
                                    break;
                                case Cursor.FIELD_TYPE_BLOB:
                                    obj.put(column, cursor.getString(i));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    obj.put(column, cursor.getFloat(i));
                                    break;
                                case Cursor.FIELD_TYPE_INTEGER:
                                    obj.put(column, cursor.getInt(i));
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    String str = cursor.getString(i);
                                    if (isValidJSONArray(str)) {
                                        try {
                                            obj.put(column, new JSONArray(str));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    } else if (isValidJSONObject(str)) {
                                        try {
                                            obj.put(column, new JSONObject(str));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        obj.put(column, cursor.getString(i));
                                    }
                                    break;
                            }
                        }
                        //Log.v(TAG, "END COLUMN NAME ===============");
                        list.add(obj);
                    } catch (InstantiationException e) {
                        Log.e(TAG, "InstantiationException: " + e.getMessage());
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        Log.e(TAG, "IllegalAccessException: " + e.getMessage());
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        Log.e(TAG, "NoSuchMethodException: " + e.getMessage());
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        Log.e(TAG, "InvocationTargetException: " + e.getMessage());
                        e.printStackTrace();
                    }

                }

                pos++;

            } while (cursor.moveToNext());

        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        if (callback != null) {
            callback.done(list);
            return null;
        } else {
            return list;
        }
    }

    @Override
    public void limit(int max) {
        limitMax = max;
        limit = " LIMIT " + max;
    }

    @Override
    public void limit(int min, int max) {
        limitMin = min;
        limitMax = max;
        limit = " LIMIT " + min + ", " + max;
    }

    @Override
    public JSONObject jsonObject() {

        JSONObject json = new JSONObject();
        if (content == null) return null;

        Set<String> keys = content.keySet();

        for (String key : keys) {

            try {

                String type = dataType.get(key);

                if (type.equals(RotLiteConsts.DATA_TYPE_STRING) || type.equals(RotLiteConsts.DATA_TYPE_DATE) || type.equals(RotLiteConsts.DATA_TYPE_DATETIME)) {
                    String v = content.getAsString(key);
                    json.put(key, v);
                } else if (type.equals(RotLiteConsts.DATA_TYPE_INTEGER)) {
                    int v = content.getAsInteger(key);
                    json.put(key, v);
                } else if (type.equals(RotLiteConsts.DATA_TYPE_DOUBLE)) {
                    double v = content.getAsDouble(key);
                    json.put(key, v);
                } else if (type.equals(RotLiteConsts.DATA_TYPE_LONG)) {
                    long v = content.getAsLong(key);
                    json.put(key, v);
                } else if (type.equals(RotLiteConsts.DATA_TYPE_BOOLEAN)) {
                    boolean v = content.getAsBoolean(key);
                    json.put(key, v);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Get value error: " + e.getMessage());
            }

        }

        return json;
    }

    @Override
    public String jsonString() {

        JSONObject json = jsonObject();
        String jstring = json.toString();

        return jstring;
    }

    private JSONObject jsonObjectToCloud() {

//        try {
//            json.put("rotlite_where", where);
//            json.put("rotlite_limit", limit);
//            json.put("rotlite_order", order);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        if (content == null) return new JSONObject();

        JSONObject json = new JSONObject();

        /*if (this instanceof RotLiteCustomModelToJson){
            json = ((RotLiteCustomModelToJson) this).toJson();
        }else {
            json = getDefaultJson();
        }*/

        json = getDefaultJson();

        Log.v("logjson", json.toString());

        return json;
    }

    @NonNull
    protected JSONObject getDefaultJson() {
        JSONObject json = new JSONObject();

        Set<String> keys = content.keySet();

        for (String key : keys) {

            try {

                String type = dataType.get(key);

                if (type.equals(RotLiteConsts.DATA_TYPE_STRING) || type.equals(RotLiteConsts.DATA_TYPE_DATE) || type.equals(RotLiteConsts.DATA_TYPE_DATETIME)) {
                    String v = content.getAsString(key);

                    if (isValidJSONArray(v)) {
                        Log.v("x-dev", "JSONArray: " + v);
                        json.put(key, new JSONArray(v));
                    }else if (isValidJSONObject(v)) {
                        Log.v("x-dev", "JSONObject: " + v);
                        json.put(key, new JSONObject(v));
                    }else {
                        json.put(key, v);
                    }

                } else if (type.equals(RotLiteConsts.DATA_TYPE_INTEGER)) {
                    int v = content.getAsInteger(key);
                    json.put(key, v);
                } else if (type.equals(RotLiteConsts.DATA_TYPE_DOUBLE)) {
                    double v = content.getAsDouble(key);
                    json.put(key, v);
                } else if (type.equals(RotLiteConsts.DATA_TYPE_LONG)) {
                    long v = content.getAsLong(key);
                    json.put(key, v);
                } else if (type.equals(RotLiteConsts.DATA_TYPE_BOOLEAN)) {
                    boolean v = content.getAsBoolean(key);
                    json.put(key, v);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Get value error: " + e.getMessage());
            }

        }
        return json;
    }

    @Override
    public void getById(String uuid) {

        where = where + " AND " + DEFAULT_ID_FIELD_NAME + " ='" + uuid + "'";
        _id = uuid;
        getById = true;

    }

    @Override
    public String getExecutedQuery() {
        return query;
    }

    @Override
    public void order(String order) {
        this.order = " ORDER BY " + order;
    }

    @Override
    public void group(String column) {
        this.group = " GROUP BY " + column;
    }

    @Override
    public boolean delete() {
        method = RotLiteConsts.METHOD_DELETE;
        if (!_id.equals("") || getById) {
            if (activityLogs) Log.v(TAG_ACTIVITY, "Data deleted by id '" + _id + "'");
            return db.delete(name, DEFAULT_ID_FIELD_NAME + "='" + _id + "'", null) > 0;
        } else if (!where.equals("1 = 1")) {
            if (activityLogs) Log.v(TAG_ACTIVITY, "Data deleted by query '" + where + "'");
            return db.delete(name, where, null) > 0;
        } else {
            if (activityLogs) Log.e(TAG_ACTIVITY, "Error deleting data");
            return false;
        }
    }

    @Override
    public boolean update() {
        method = RotLiteConsts.METHOD_UPDATE;
        boolean updated;

        try {

            if (!_id.equals("") || getById) {
                updatedAt();
                if (activityLogs) Log.v(TAG_ACTIVITY, "Data updated by id '" + _id + "' " + jsonString());
                updated = db.update(name, content, DEFAULT_ID_FIELD_NAME + "='" + _id + "'", null) > 0;
            } else if (!where.equals("1 = 1")) {
                updatedAt();
                Log.v(TAG_ACTIVITY, "Data updated by query '" + where + "'");
                updated = db.update(name, content, where, null) > 0;
            } else {
                Log.v(TAG_ACTIVITY, "Error updating data " + jsonString());
                updated = false;
            }
        }catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error on update " + e.getMessage());
            updated = false;
        }
        return updated;
    }

    @Override
    public void showActivityLogs(boolean activity) {
        activityLogs = activity;
    }

    @Override
    public void fromLocal() {
        this.from = RotLiteConsts.FROM_LOCAL;
    }

    @Override
    public void fromWeb() {
        this.from = RotLiteConsts.FROM_WEB;
    }

    @Override
    public void setEndpoint(String name) {

    }

    @Override
    public void setEndpointPost(String name) {

    }

    @Override
    public void setEndpointPut(String name) {

    }

    @Override
    public void setEndpointDelete(String name) {

    }

    @Override
    public String getEndpoint() {
        return this.endpoint;
    }

    /*@Override
    public void getObject() throws RotLiteException {

        if (this.from == RotLiteConsts.FROM_LOCAL) {

            String sql = "SELECT * FROM " + name + " WHERE " + where;

            if (!order.equals("")) {
                sql = sql + order;
            }

            if (!limit.equals("")) {
                sql = sql + limit;
            }

            query = sql;

            try {
                Cursor cursor = db.rawQuery(query, null);
                if (cursor.moveToFirst()) {

                    String[] cols = cursor.getColumnNames();
                    setId(cursor.getString(1));
                    setLocalId(cursor.getInt(0));

                    for (int i = 0; i < cols.length; i++) {

                        put(cols[i], cursor.getString(i));

                    }

                }

                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }

            } catch (SQLiteDatabaseCorruptException e) {
                new RotLiteException("SQLiteDatabaseCorruptException " + e.getMessage().toString() + "", e.hashCode());
            } catch (android.database.sqlite.SQLiteConstraintException e) {
                new RotLiteException("SQLiteConstraintException " + e.getMessage(), e.hashCode());
            } catch (SQLiteException e) {
                new RotLiteException("SQLiteException " + e.getMessage(), e.hashCode());
            } catch (Exception e) {
                new RotLiteException("Exception " + e.getMessage(), e.hashCode());
            }

        }

    }

    @Override
    public void getObjectInBackground() throws RotLiteException {

        if (this.from == RotLiteConsts.FROM_LOCAL) {

            String sql = "SELECT * FROM " + name + " WHERE " + where;

            if (!order.equals("")) {
                sql = sql + order;
            }

            if (!limit.equals("")) {
                sql = sql + limit;
            }

            query = sql;

            try {

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        Cursor cursor = db.rawQuery(query, null);
                        if (cursor.moveToFirst()) {

                            String[] cols = cursor.getColumnNames();

                            for (int i = 0; i < cols.length; i++) {

                                put(cols[i], cursor.getString(i));

                            }

                        }

                        if (cursor != null && !cursor.isClosed()) {
                            cursor.close();
                        }

                    }
                };
                new Thread(runnable).start();
            } catch (SQLiteDatabaseCorruptException e) {
                new RotLiteException("SQLiteDatabaseCorruptException " + e.getMessage().toString() + "", e.hashCode());
            } catch (android.database.sqlite.SQLiteConstraintException e) {
                new RotLiteException("SQLiteConstraintException " + e.getMessage(), e.hashCode());
            } catch (SQLiteException e) {
                new RotLiteException("SQLiteException " + e.getMessage(), e.hashCode());
            } catch (Exception e) {
                new RotLiteException("Exception " + e.getMessage(), e.hashCode());
            }

        }

    }*/

    //@Override
    public T getObject(String column) {
        //Log.v(TAG, "column: " + column + "; tem a key? " + includedModels.containsKey(column) + "; id " + includedModels.get(column).getLocalId());
        return (includedModels != null && includedModels.containsKey(column) && includedModels.get(column).getLocalId() > 0 ? includedModels.get(column) : null);
    }

    @Override
    public void setDateFormat(String format) {
        DATE_FORMAT = format;
    }

    @Override
    public String getDateFormat() {
        return DATE_FORMAT;
    }

    public Context getContext(){
        return this.context;
    }

    @Override
    public void setConverter(RotLiteConverter converter) {
        this.converter = converter;
    }

    @Override
    public RotLiteConverter getConverter() {
        return converter;
    }

    @Override
    public void setDefaultIdFieldName(String name) {
        DEFAULT_ID_FIELD_NAME = name;
    }

    @Override
    public String getDefaultIdFieldName() {
        return DEFAULT_ID_FIELD_NAME;
    }

    @Override
    public void prepareSync() {
    }

    @Override
    public void onReceiveSyncData(List data) {
    }

    @Override
    public List beforeSync(List list) {
        return list;
    }

    @Override
    public void onSync(List synchronizedList) {

    }

    @Override
    public void onSync(List synchronizedList, List unsynchronizedList) {

    }

    @Override
    public void sync() throws RotLiteException {
        Log.v(TAG, "saveweb sync");
        syncData(null);
    }

    @Override
    public void syncInBackground(final SyncCallback callback) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    syncData(callback);
                } catch (RotLiteException e) {
                    e.printStackTrace();
                    callback.onFailure(e);
                }
            }
        };
        new Thread(runnable).start();

    }

    private void syncData(final SyncCallback callback) throws RotLiteException {

        Log.v(TAG, "Starting sync...");

        final boolean[] hasError = {false};
        final boolean[] downloadRunning = {true};
        final boolean uploadRunning = true;
        final List<RotLiteModel>[] objsToUpload = new List[]{new ArrayList<RotLiteModel>()};

        try {

            //Downloading data...
            prepareSync();
            fromWeb();

            findInBackground(new RotLiteCallback<T>() {
                @Override
                public void done(List<T> list) {

                    Log.d(TAG, String.format("Found inda web %d", list != null ? list.size() : 0));

                    if (list == null) list = new ArrayList<>();

                    List<T> aux = new ArrayList<>();

                    for (int i = list.size() - 1; i >= 0; i--) {
                        RotLiteObject obj = list.get(i);
                        list.remove(i);
                        try {
                            T newModel;
                            try {
                                newModel = classInstance.getDeclaredConstructor(Context.class).newInstance(context);
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

                    if (!getAutoSync()) {

                        onReceiveSyncData(aux);

                    } else {

                        List<T> objs = beforeSync(aux);

                        if (objs != null) {
                            if (objs.size() != 0) {
//                                List<T> synchronizedObjs = new ArrayList<>();
//                                List<T> unsynchronizedObjs = new ArrayList<>();
                                for (int i = 0; i < objs.size(); i++) {

                                    T obj = objs.get(i);

                                    obj.setIsSync(true);

                                    try {
                                        obj.saveLocal();
//                                        synchronizedObjs.add(obj);
                                    } catch (Exception e) {
//                                        unsynchronizedObjs.add(obj);
                                        e.printStackTrace();
                                    }

                                }

                                java.text.DateFormat dateFormat = new SimpleDateFormat(getDateFormat());
                                Date date = new Date();

                                setLastSyncDate(dateFormat.format(date));

                                //TODO: Refactor - JSON gigante, estoura memoria
                                onSync(null);
//                                onSync(synchronizedObjs);
//                                if (unsynchronizedObjs.size() > 0)
//                                    onSync(synchronizedObjs, unsynchronizedObjs);

                                downloadRunning[0] = false;

                                if (objsToUpload[0].size() <= 0 && downloadRunning[0] == false) {
                                    callback.onSuccess();
                                }

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
                    callback.onFailure(e);
                    hasError[0] = true;
                }
            });

            final String classInstanceName = this.classInstance.getName();

            RotLiteModel toUpload = new RotLiteModel(context);
            toUpload.where("model = '" + classInstanceName + "'");
            toUpload.findInBackground(new RotLiteCallback<RotLiteModel>() {
                @Override
                public void done(List<RotLiteModel> list) {
                    Log.v(TAG, "To upload: " + list.size());

                    objsToUpload[0] = list;

                    for (int i = 0; i < list.size(); i++) {

                        final int pos = i;

                        final RotLiteModel model = list.get(i);

                        getById(model.getString("data_id"));

                        try {
                            List<T> find = find();
                            if (find.size() > 0) {
                                final T obj = find.get(0);
                                obj.saveWeb(new RotLiteSyncUploadCallback() {
                                    @Override
                                    public void onFailure(RotLiteException e) {
                                        Log.e(TAG, "Failure to upload '" + obj.getId() + "': " + e.getMessage());
                                        callback.onFailure(e);
                                        hasError[0] = true;
                                    }

                                    @Override
                                    public void onSuccess(Response response, RotLiteObject objUpload) {
                                        Log.v(TAG, "Success to upload '" + objUpload.getId() + "': " + objUpload.jsonString());
                                        RotLiteModel temp = new RotLiteModel(context);
                                        temp.where("model = '" + classInstanceName + "' and data_id = '" + obj.getId() + "'");
                                        if (temp.delete()) {
                                            Log.v("deleted", "3true " + jsonString());
                                        }else{
                                            Log.v("deleted", "3false " + jsonString());
                                        }
                                        obj.setIsSync(true);
                                        if (obj.update()) {
                                            Log.v("setSync", "ops2 ");
                                        }else{
                                            Log.v("setSync", "opa..2");
                                        }
                                        objsToUpload[0].remove(pos);

                                        if (objsToUpload[0].size() <= 0 && downloadRunning[0] == false) {
                                            callback.onSuccess();
                                        }

                                    }
                                });
                            }
                        } catch (RotLiteException e) {
                            e.printStackTrace();
                        }

                    }

                }

                @Override
                public void error(RotLiteException e) {
                    callback.onFailure(e);
                    hasError[0] = true;
                }
            });
        } finally {
            if (!hasError[0]) {
                //Por padrão devemos voltar a usar o storage local
                fromLocal();

            }else{
                Log.e(TAG, "deu ruim");
                callback.onFailure(null);
            }
        }

    }

    @Override
    public String getLastSyncDate() {
        String date = null;
        if (sPrefs != null) {
            date = sPrefs.getString(name + "_last_sync_date", null);
        }
        return date;
    }

    @Override
    public void setLastSyncDate(String date) {
        if (sPrefs != null) {
            SharedPreferences.Editor editor = sPrefs.edit();
            editor.putString(name + "_last_sync_date", date);
            editor.commit();
        }
    }

    @Override
    public boolean isSync() {
        return getBoolean(ROTLITE_IS_SYNC);
    }

    @Override
    public void setIsSync(Boolean isSync) {
        put(ROTLITE_IS_SYNC, isSync);
    }

    public static String getClassName() {
        return classInstanceName;
    }

    private Call saveData(String url, String json, Callback callback) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);

        Request.Builder builder = new Request.Builder();
        builder.url(url);

        if (method == RotLiteConsts.METHOD_INSERT) {
            builder.post(body);
        } else if (method == RotLiteConsts.METHOD_UPDATE) {
            builder.put(body);
        } else if (method == RotLiteConsts.METHOD_DELETE) {
            builder.delete(body);
        }

        Request request = builder.build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private Call saveData(JSONObject object, Callback callback) throws IOException {

        FormEncodingBuilder form = new FormEncodingBuilder();
        JSONArray names = object.names();

        for (int i = 0; i < object.length(); i++) {

            String name = "";

            try {

                name = names.getString(i);

                try {

                    form.add(name, object.getString(name));

                } catch (Exception e) {

                    try {

                        form.add(name, String.valueOf(object.getInt(name)));

                    } catch (Exception e2) {

                        try {
                            form.add(name, String.valueOf(object.getDouble(name)));
                        } catch (Exception e3) {
                            try {
                                form.add(name, String.valueOf(object.getLong(name)));
                            } catch (Exception e4) {

                                try {
                                    form.add(name, String.valueOf(object.getBoolean(name)));
                                } catch (Exception e5) {
                                    Log.e(TAG, "Get value error: " + e5.getMessage());
                                    e5.printStackTrace();
                                }

                            }
                        }

                    }

                }

            } catch (JSONException e) {
                Log.e(TAG, "JSONException: " + e.getMessage());
                e.printStackTrace();
            }

        }

        RequestBody formBody = form.build();

        Request.Builder builder = new Request.Builder();

        String myUrl = url;

        //Log.v("poijson", object.toString());

        //TODO: Melhorar duplicação de código abaixo
        RequestBody jsonBody = RequestBody.create(JSON, object.toString());
        if (method == RotLiteConsts.METHOD_INSERT) {

            if (getBodyDataFormat().equals(RotLite.BODY_DATA_JSON)) {
                builder.post(jsonBody);
            }else if (getBodyDataFormat().equals(RotLite.BODY_DATA_FORM)) {
                builder.post(formBody);
            }

            myUrl = url + endpointPost;
            Log.v(TAG, "post! " + getId());
        } else if (method == RotLiteConsts.METHOD_UPDATE) {

            if (getBodyDataFormat().equals(RotLite.BODY_DATA_JSON)) {
                builder.put(jsonBody);
            }else if (getBodyDataFormat().equals(RotLite.BODY_DATA_FORM)) {
                builder.put(formBody);
            }

            myUrl = url + endpointPut;
            Log.v(TAG, "update! " + getId());
        } else if (method == RotLiteConsts.METHOD_DELETE) {

            if (getBodyDataFormat().equals(RotLite.BODY_DATA_JSON)) {
                builder.delete(jsonBody);
            }else if (getBodyDataFormat().equals(RotLite.BODY_DATA_FORM)) {
                builder.delete(formBody);
            }

            myUrl = url + endpointDelete;
        }

        Log.v(TAG, "Url: " + myUrl);

        builder.header("Accept", "application/json");
        builder.header("Content-type", "application/json");

        builder.url(myUrl);

        Request request = builder.build();

        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private Call getData(JSONObject object, Callback callback) throws IOException {

        FormEncodingBuilder form = new FormEncodingBuilder();

        if (endpoint != null && !endpoint.equals("")) {
            String getUrl = url + endpoint + "?";

            if (object != null) {
                JSONArray names = object.names();

                for (int i = 0; i < object.length(); i++) {

                    String name = "";

                    try {

                        name = names.getString(i);

                        try {

                            getUrl = getUrl + name + "=" + URLEncoder.encode(object.getString(name), "UTF-8") + "&";

                        } catch (Exception e) {

                            try {

                                getUrl = getUrl + name + "=" + URLEncoder.encode(String.valueOf(object.getInt(name)), "UTF-8") + "&";

                            } catch (Exception e2) {

                                try {
                                    getUrl = getUrl + name + "=" + URLEncoder.encode(String.valueOf(object.getDouble(name)), "UTF-8") + "&";
                                } catch (Exception e3) {
                                    try {
                                        getUrl = getUrl + name + "=" + URLEncoder.encode(String.valueOf(object.getLong(name)), "UTF-8") + "&";
                                    } catch (Exception e4) {

                                        try {
                                            getUrl = getUrl + name + "=" + URLEncoder.encode(String.valueOf(object.getBoolean(name)), "UTF-8") + "&";
                                        } catch (Exception e5) {
                                            Log.e(TAG, "Get value error: " + e5.getMessage());
                                            e5.printStackTrace();
                                        }

                                    }
                                }

                            }

                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException: " + e.getMessage());
                        e.printStackTrace();
                    }

                }

            }

            getUrl = getUrl.substring(0, getUrl.length() - 1);
//            Log.d(TAG, "getUrl before: " + getUrl);
//            getUrl = getUrl + "&perPage=500&page=1";
//            Log.d(TAG, "getUrl after: " + getUrl);


            StringBuilder log = new StringBuilder();
            log.append("Diff url with page query");
            log.append("\nBEFORE: " + getUrl);
            getUrl = appendQueryPage(getUrl);
            log.append("\nAFTER: " + getUrl);
            Log.v("MADMAX", log.toString());

            Request.Builder builder = new Request.Builder();
            builder.url(getUrl);

            Log.v(TAG, "URL: " + getUrl);

            Request request = builder.build();

            Call call = client.newCall(request);
            if (callback != null) {
                call.enqueue(callback);
            }
            return call;
        }else{
            return null;
        }
    }

    // Append, page parameters to url
    private String appendQueryPage(String getUrl) {
        if (table != null && !table.perPageQuery().equals("") && !table.pageQuery().equals("")){
            getUrl = getUrl + "&" + table.perPageQuery() + "=" + table.perPageCount() + "&" + table.pageQuery() + "=" + Integer.toString(currentPage);//TODO: Paginação, mudar as páginas
        }
        return getUrl;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return manager.getActiveNetworkInfo() != null &&
                manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void createdAt() {
        final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final Date date = new Date();

        if (!content.containsKey("createdAt")) {
            this.putDateTime("createdAt", dateFormat.format(date));
        }

        updatedAt();
    }

    private void updatedAt() {
        final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final Date date = new Date();
        //if (!content.containsKey("updatedAt")) {
            this.putDateTime("updatedAt", dateFormat.format(date));
        //}
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static BroadcastReceiver getReceiver(RotLiteBroadcastReceiver broadcast) {
        if (broadcast != null) { broadcastReceiver = broadcast; }
        return myReceiver;
    }
    public static BroadcastReceiver getReceiver() {
        return myReceiver;
    }

    // Define the callback for what to do when data is received
    public static BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String model = intent.getStringExtra("model");
            String action = intent.getStringExtra("action");

            Log.v("broadcast", "model: " + model + "; action: " + action);

            if (broadcastReceiver != null) {
                if (model != null && model.equals(getClassName())) {
                    if (action.equals("download")) {
                        broadcastReceiver.onFinishDownloadSync();
                    } else if (action.equals("upload")) {
                        broadcastReceiver.onFinishUploadSync();
                    }
                }
            }

        }
    };

    private boolean isValidJSONArray(String jsonArray) {

        try {
            if (jsonArray != null) {
                JSONArray array = new JSONArray(jsonArray);
                return true;
            }else{
                return false;
            }
        } catch (JSONException e) {
            return false;
        }

    }

    private boolean isValidJSONObject(String jsonObject) {

        try {
            if (jsonObject != null) {
                JSONObject array = new JSONObject(jsonObject);
                return true;
            }else{
                return false;
            }
        } catch (JSONException e) {
            return false;
        }

    }

}