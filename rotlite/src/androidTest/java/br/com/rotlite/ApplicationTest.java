package br.com.rotlite;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.ApplicationTestCase;
import android.util.Log;

import java.util.ArrayList;

import br.com.rotlite.rotlite.ForeignKey;
import br.com.rotlite.rotlite.RotLite;
import br.com.rotlite.rotlite.RotLiteObject;
import br.com.rotlite.rotlite.Table;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    private static String TAG = "rotLiteTest";

    SQLiteDatabase db;

    public void testCreateRelation(){

        Log.v(TAG, "running test");
        System.out.println("This is System.out.println");


        db = RotLite.getInstance().getDataBase(getContext());

        //for (int i = 0; i < 10; i++) {
            Categorias cat = new Categorias(getContext());
            cat.put("categoria", "cat test");
            try {
                cat.saveLocal();
                Log.v(TAG, "Categoria salva: " + cat.jsonString());
            } catch (Exception e) {
                e.printStackTrace();
                assertEquals(e.getMessage(), false, true);
            }
        //}


        Clientes cliente = new Clientes(getContext());
        cliente.put("nome", "Claudio");
        cliente.put("categoria", "sakd");

        try {
            cliente.saveLocal();
            Log.v(TAG, "Cliente salvo: " + cliente.jsonString());
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(e.getMessage(), false, true);
        }

        Log.v(TAG, "show table details:");

        boolean thrown = false;
        try {
            db.execSQL("INSERT INTO clientes (nome, categoria) VALUES ('usuario', 'asdalskd')");
        }catch(SQLiteException e){
            thrown = true;
            assertEquals(e.getMessage(), false, thrown);
        }
        assertTrue(thrown);

        //assertEquals(db, 1, 2);

    }

    public void testTableForeignKey() {



    }

    @Table(name = "clientes", autosync = true)
    public class Clientes extends RotLiteObject {

        @ForeignKey(column = "refund_id", references = "uuid")
        Categorias categorias;

        public Clientes(Context context) {
            super(context, Clientes.class);
        }

    }

    @Table(name = "categorias", autosync = true)
    public class Categorias extends RotLiteObject {

        public Categorias(Context context) {
            super(context, Categorias.class);
        }

    }


}