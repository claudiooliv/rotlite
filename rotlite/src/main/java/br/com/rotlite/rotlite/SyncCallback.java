package br.com.rotlite.rotlite;

/**
 * Created by claudio on 04/10/15.
 */
public interface SyncCallback {
    void onSuccess();
    void onFailure(RotLiteException e);
}
