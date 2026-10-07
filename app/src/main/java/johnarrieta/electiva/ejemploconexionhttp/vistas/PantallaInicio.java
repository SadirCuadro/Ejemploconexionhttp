package johnarrieta.electiva.ejemploconexionhttp.vistas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import johnarrieta.electiva.ejemploconexionhttp.R;
import johnarrieta.electiva.ejemploconexionhttp.controladores.ConexionHttpPostServer;
import johnarrieta.electiva.ejemploconexionhttp.datos.Usuario;

public class PantallaInicio extends AppCompatActivity {

    private EditText campoEmail;
    private EditText campoClave;
    private Button btnCancelar;
    private Button btnIniciarSesion;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pantalla_inicio);

        campoEmail = findViewById(R.id.campoEmail);
        campoClave = findViewById(R.id.campoClave);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);

        // Botón para ir a registrar un nuevo usuario
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PantallaInicio.this, PantallaCrudUsuario.class);
                startActivity(intent);
            }
        });

        // Botón para iniciar sesión
        btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarYIniciarSesion();
            }
        });
    }

    private void validarYIniciarSesion() {
        String email = campoEmail.getText().toString().trim();
        String clave = campoClave.getText().toString().trim();

        if (email.isEmpty()) {
            campoEmail.setError("El correo es requerido");
            campoEmail.requestFocus();
            return;
        }

        if (clave.isEmpty()) {
            campoClave.setError("La contraseña es requerida");
            campoClave.requestFocus();
            return;
        }

        ejecutarPeticionLogin(email, clave);
    }

    private void ejecutarPeticionLogin(String email, String clave) {
        btnIniciarSesion.setEnabled(false);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("accion", "login");
                parametros.put("email", email);
                parametros.put("psw", clave);

                String respuesta;
                try {
                    respuesta = ConexionHttpPostServer.enviarPost(parametros);
                } catch (Exception e) {
                    respuesta = null;
                }

                final String respuestaFinal = respuesta;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        btnIniciarSesion.setEnabled(true);
                        procesarRespuestaLogin(respuestaFinal, clave);
                    }
                });
            }
        });
    }

    private void procesarRespuestaLogin(String respuestaJson, String claveIngresada) {
        if (respuestaJson == null || respuestaJson.trim().isEmpty()) {
            Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Usuario usuario = null;
            String limpio = respuestaJson.trim();

            if (limpio.startsWith("[")) {
                Type tipoLista = new TypeToken<List<Usuario>>() {}.getType();
                List<Usuario> lista = gson.fromJson(limpio, tipoLista);
                if (lista != null && !lista.isEmpty()) {
                    usuario = lista.get(0);
                }
            } else if (limpio.startsWith("{")) {
                usuario = gson.fromJson(limpio, Usuario.class);
            }

            if (usuario != null && usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
                if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
                    usuario.setPassword(claveIngresada);
                }

                // Guardar datos en SharedPreferences para persistencia de sesión
                SharedPreferences preferences = getSharedPreferences("sesion_usuario", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("email", usuario.getEmail());
                editor.putString("password", usuario.getPassword());
                editor.putString("nombre", usuario.getNombre());
                editor.apply();

                Toast.makeText(this, "Bienvenido " + usuario.getNombre(), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(PantallaInicio.this, PantallaListado.class);
                intent.putExtra("usuario", usuario);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Credenciales incorrectas o usuario no encontrado", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar la respuesta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
