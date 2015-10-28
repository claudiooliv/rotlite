package br.com.rotlite.rotlite;

import android.content.Context;

/**
 * Created by claudio on 30/08/15.
 */
@Table(name = "rotlite_objects_storage")
public class RotLiteModel extends RotLiteObject {

    public RotLiteModel(Context context) {
        super(context, RotLiteModel.class);
    }

}