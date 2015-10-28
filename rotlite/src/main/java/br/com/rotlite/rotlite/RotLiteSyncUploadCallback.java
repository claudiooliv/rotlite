package br.com.rotlite.rotlite;

import com.squareup.okhttp.Response;

/**
 * Created by danillosantos on 02/10/15.
 */
public interface RotLiteSyncUploadCallback<T extends RotLiteObject> {
    void onFailure(RotLiteException e);
    void onSuccess(Response response, T obj);
}
