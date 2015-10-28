package br.com.rotlite.rotlite;

import android.util.JsonReader;
import android.util.JsonToken;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by danillosantos on 28/09/15.
 */
public interface RotLiteConverterInterface<T extends RotLiteObject> {

    void setResponseString(String responseString) throws Exception;
    void setObjectsFrom(String data, String type);
    JsonArray getObjects();
    JsonObject getObject();
    JsonElement getElement(String key);
    String getString(String key);
    String getTypeReceivedData();
    String getTypeObjectsDataReceived();
    void setJsonReader(JsonReader reader);
    void getJsonReader(RotLiteConverter.DataObjects data);
    JSONObject getJSONObject(String element);
    JSONArray getJSONArray(String element);
    void setJsonReaderData(JsonReader reader);
    JsonReader getJsonReaderData();

}
