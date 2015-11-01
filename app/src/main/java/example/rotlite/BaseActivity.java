package example.rotlite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import example.rotlite.models.Usuarios;
import example.rotlite.utils.Consts;

/**
 * Created by claudio on 19/09/15.
 */
public class BaseActivity extends AppCompatActivity {

    public static String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RotLite.getInstance().start(this);
    }

    public void verifyLogged() {
        Log.v(TAG, "verifyLogged");

        //Verifica se o usuário está logado
        Usuarios user = new Usuarios(this);
        if (user.isLogged()) {
            Intent home = new Intent(this, HomeActivity.class);
            startActivity(home);
            finish();
        }

    }

    public Usuarios getCurrentUser(Context context) {

        Usuarios user = new Usuarios(context);
        user.where(Consts.USER_LOGGED + " = 1");
        try {
            user.getObject();
            return user;
        } catch (RotLiteException e) {
            e.printStackTrace();
            return null;
        }

    }
}
