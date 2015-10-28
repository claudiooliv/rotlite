package br.com.rotlite.rotlite;

import java.util.List;

/**
 * Created by claudio on 12/08/15.
 */
public interface RotLiteCallback<T extends RotLiteObject> {
    void done(List<T> list);
    void error(RotLiteException e);
}
