package example.rotlite;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import example.rotlite.models.Usuarios;
import example.rotlite.rotlite.RotLite;
import example.rotlite.rotlite.RotLiteException;
import example.rotlite.utils.Consts;

public class RegisterActivity extends BaseActivity {

    Context context;

    EditText nome, email, senha, confirmarSenha;
    Button criarConta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        context = this;

        nome = (EditText) findViewById(R.id.nome);
        email = (EditText) findViewById(R.id.email);
        senha = (EditText) findViewById(R.id.senha);
        confirmarSenha = (EditText) findViewById(R.id.confirmarSenha);
        criarConta = (Button) findViewById(R.id.criarConta);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        criarConta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String strNome = nome.getText().toString();
                String strEmail = email.getText().toString();
                String strSenha = senha.getText().toString();
                String strConfirmarSenha = confirmarSenha.getText().toString();

                if (strNome.equals("")) {
                    Toast.makeText(RegisterActivity.this, "Preencha o campo 'nome'", Toast.LENGTH_SHORT).show();
                    nome.requestFocus();
                }else if (strEmail.equals("")) {
                    Toast.makeText(RegisterActivity.this, "Preencha o campo 'e-mail'", Toast.LENGTH_SHORT).show();
                    email.requestFocus();
                }else if (strSenha.equals("")) {
                    Toast.makeText(RegisterActivity.this, "Preencha o campo 'senha'", Toast.LENGTH_SHORT).show();
                    senha.requestFocus();
                }else if (!strSenha.equals(strConfirmarSenha)) {
                    Toast.makeText(RegisterActivity.this, "As senhas não conferem", Toast.LENGTH_SHORT).show();
                    confirmarSenha.requestFocus();
                }else{

                    if (RotLite.isOnline(RegisterActivity.this)) {

                        Usuarios user = new Usuarios(RegisterActivity.this);
                        user.put(Consts.USER_NAME, strNome);
                        user.put(Consts.USER_EMAIL, strEmail);
                        user.put(Consts.USER_PASSWORD, strSenha);

                        try {
                            user.save();
                        }catch(Exception e) {
                            Log.e(TAG, e.getMessage());
                        }

                    }else{
                        Toast.makeText(RegisterActivity.this, "Sem conexão com a internet", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
