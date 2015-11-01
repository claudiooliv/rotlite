package example.rotlite.models;

import android.content.Context;

import example.rotlite.rotlite.RotLiteObject;
import example.rotlite.rotlite.Table;

/**
 * Created by claudio on 12/09/15.
 */
@Table(name = "tarefas", autosync = true)
public class Tarefas extends RotLiteObject {

    public Tarefas(Context context) {
        super(context, Tarefas.class);
    }

}

