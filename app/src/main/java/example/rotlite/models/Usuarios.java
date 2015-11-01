package example.rotlite.models;

import android.content.Context;
import android.util.Log;

import java.util.List;

import example.rotlite.rotlite.RotLiteCallback;
import example.rotlite.rotlite.RotLiteException;
import example.rotlite.rotlite.RotLiteObject;
import example.rotlite.rotlite.Table;
import example.rotlite.utils.Consts;

/**
 * Created by claudio on 20/08/15.
 */
@Table(name = "usuarios", autosync = false)
public class Usuarios extends RotLiteObject {

    public Usuarios(Context context) {
        super(context, Usuarios.class);
    }

    public boolean isLogged() {

        final boolean[] result = {false};

        this.where(Consts.USER_LOGGED + " = 1");

        this.find(new RotLiteCallback<List<RotLiteObject>>() {
            @Override
            public void done(List<RotLiteObject> list) {

                if (list.size() > 0) {
                    result[0] = true;
                } else {
                    result[0] = false;
                }

            }

            @Override
            public void error(RotLiteException e) {
                Log.e(MODEL_TAG, e.getMessage());
            }
        });

        return result[0];

    }

}
