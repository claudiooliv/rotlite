package br.com.rotlite.rotlite;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.util.List;

/**
 * Created by claudio on 24/09/15.
 */
public interface RotLiteSyncCallback<T extends RotLiteObject> {
    void onSuccessLocal(T obj);
    void onFailureLocal(RotLiteException e);
    void onSuccessCloud(Response response, T obj);
    void onFailureCloud(RotLiteException e);
}