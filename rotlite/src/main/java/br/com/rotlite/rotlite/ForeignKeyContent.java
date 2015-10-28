package br.com.rotlite.rotlite;

import android.content.Context;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by danillosantos on 21/10/15.
 */
public class ForeignKeyContent<T extends  RotLiteObject> {

    Class<T> classModel;
    String column, references;
    Context context;

    public ForeignKeyContent(Class<T> clzz, String col, String ref, Context ctx) {
        classModel = clzz;
        column = col;
        references = ref;
        context = ctx;
    }

    public String getColumn() {
        return column;
    }

    public String getReferences() {
        return references;
    }

    public Class<T> getClassModel() {
        return classModel;
    }

    public String getTableName() {

        if (classModel != null) {

            try {
                T obj = classModel.getDeclaredConstructor(Context.class).newInstance(context);
                return obj.getTbName();
            } catch (InstantiationException e) {
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return null;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            }

        }else {
            return null;
        }
    }

    public T getModel() {
        if (classModel != null) {
            try {
                return classModel.getDeclaredConstructor(Context.class).newInstance(context);
            } catch (InstantiationException e) {
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return null;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

}
