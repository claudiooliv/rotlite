package br.com.rotlite.rotlite;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Claudio Oliveira on 28/09/15.
 */
public class RotLiteConverter<T extends RotLiteObject> implements RotLiteConverterInterface {

    private Response response;
    private String responseStr;
    private String objectsFrom = "";
    public static String JSON_ARRAY = "array";
    public static String JSON_OBJECT = "object";
    private String typeReceivedData, typeObjectsReceivedData;
    public static String TAG = RotLiteConverter.class.getSimpleName();
    private JsonObject responseObject;
    private JsonArray responseArray;
    private JsonReader jsonReader;
    private JsonReader jsonReaderData;

    private Map<String, JSONArray> receivedArrays = new HashMap<String, JSONArray>();
    private Map<String, JSONObject> receivedObjects = new HashMap<String, JSONObject>();

    @Override
    public void setResponseString(String responseString) throws Exception {
        JsonParser jsonParser = new JsonParser();

        try {

            responseStr = responseString;
            Log.v(TAG, "Response: " + responseStr);

            try{

                typeReceivedData = JSON_OBJECT;
                responseObject = jsonParser.parse(responseStr).getAsJsonObject();

            } catch (Exception e) {
                e.printStackTrace();
                if (responseStr != null && !responseStr.equals("")) {
                    typeReceivedData = JSON_ARRAY;
                    responseArray = jsonParser.parse(responseStr).getAsJsonArray();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    @Override
    public void setObjectsFrom(String key, String type) {
        objectsFrom = key;
        typeObjectsReceivedData = type;
    }

    @Override
    public JsonArray getObjects() {
        return (getTypeReceivedData() == JSON_OBJECT ? responseObject.getAsJsonArray(objectsFrom) : responseArray );
    }

    @Override
    public JsonObject getObject() {
        return (getTypeReceivedData() == JSON_OBJECT ? responseObject : responseArray.getAsJsonObject() );
    }

    @Override
    public JsonElement getElement(String key) {
        return (responseObject != null ? responseObject.get(key) : null);
    }

    @Override
    public String getString(String key) {
        return getElement(key).getAsString();
    }

    @Override
    public String getTypeReceivedData() {
        return typeReceivedData;
    }

    @Override
    public String getTypeObjectsDataReceived() {
        return typeObjectsReceivedData;
    }

    @Override
    public void setJsonReader(JsonReader reader) {

        jsonReader = reader;

    }

    @Override
    public void getJsonReader(DataObjects data) {

        JsonToken token = null;

        try {

            token = jsonReader.peek();


            //Se recebermos um objeto do backend...
            if (token == JsonToken.BEGIN_OBJECT) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();

                    Log.v(TAG, "name: " + name + "; token: " + jsonReader.peek().name());

                    if (name.equals(objectsFrom)) {
                        token = jsonReader.peek();

                        if (token == JsonToken.BEGIN_ARRAY){
                            Log.v(TAG, "caiu aqui 4");
                            data.getData(jsonReader);
                        }else{
                            Log.v(TAG, "caiu aqui 6");
                            jsonReader.skipValue();
                        }
                        /*
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
                        }*/

                    }else{
                        JsonToken getToken = jsonReader.peek();
                        if (getToken == JsonToken.BEGIN_ARRAY) {
                            if (receivedArrays == null) receivedArrays = new HashMap<String, JSONArray>();
                            receivedArrays.put(name, RotLiteObject.getJSONArrayFromJsonReader(jsonReader));
                            Log.v(TAG, "ARRAYS SIZE " + receivedArrays.size());
                        }else if (getToken == JsonToken.BEGIN_OBJECT){
                            if (receivedObjects == null) receivedObjects = new HashMap<String, JSONObject>();
                            receivedObjects.put(name, RotLiteObject.getJSONObjectFromJsonReader(jsonReader));
                        }else{
                            jsonReader.skipValue();
                        }
                    }
                }
                jsonReader.endObject();
            }else{
                jsonReader.skipValue();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject getJSONObject(String element) {
        return null;
    }

    @Override
    public JSONArray getJSONArray(String element) {
        return (receivedArrays != null && receivedArrays.containsKey(element) ? receivedArrays.get(element) : null);
    }

    @Override
    public void setJsonReaderData(JsonReader reader) {
        jsonReaderData = reader;
    }

    @Override
    public JsonReader getJsonReaderData() {
        return jsonReaderData;
    }

    public interface DataObjects {
        void getData(JsonReader data);
    }

}
