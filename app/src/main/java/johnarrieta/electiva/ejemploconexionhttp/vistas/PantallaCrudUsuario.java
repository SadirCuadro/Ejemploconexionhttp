package johnarrieta.electiva.ejemploconexionhttp.vistas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import johnarrieta.electiva.ejemploconexionhttp.R;
import johnarrieta.electiva.ejemploconexionhttp.controladores.ConexionHttpPostServer;
import johnarrieta.electiva.ejemploconexionhttp.datos.Usuario;

public class PantallaCrudUsuario extends AppCompatActivity {

    private EditText campoCodigo2;
    private EditText campoPassword2;
    private EditText campoNombre;
    private Button botonModificar;
    private Button botonCancelar2;

    private boolean esModificacion = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pantalla_crud_usuario);

        campoCodigo2 = findViewById(R.id.campoCodigo2);
        campoPassword2 = findViewById(R.id.campoPassword2);
        campoNombre = findViewById(R.id.campoNombre);
        botonModificar = findViewById(R.id.botonModificar);
        botonCancelar2 = findViewById(R.id.botonCancelar2);

        // Verificar si se recibió un usuario para editar
        Usuario usuarioRecibido = (Usuario) getIntent().getSerializableExtra("usuario");
        if (usuarioRecibido != null) {
            esModificacion = true;
            campoCodigo2.setText(usuarioRecibido.getEmail());
            campoPassword2.setText(usuarioRecibido.getPassword());
            campoNombre.setText(usuarioRecibido.getNombre());
            botonModificar.setText("Modificar");
        } else {
            esModificacion = false;
            botonModificar.setText("Guardar");
        }

        // Botón Cancelar finaliza la actividad
        botonCancelar2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Botón Modificar / Guardar
        botonModificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ejecutarOperacionCrud();
            }
        });
    }

    private void ejecutarOperacionCrud() {
        String email = campoCodigo2.getText().toString().trim();
        String password = campoPassword2.getText().toString().trim();
        String nombre = campoNombre.getText().toString().trim();

        if (email.isEmpty()) {
            campoCodigo2.setError("Ingrese el correo / código");
            campoCodigo2.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            campoPassword2.setError("Ingrese la contraseña");
            campoPassword2.requestFocus();
            return;
        }

        if (nombre.isEmpty()) {
            campoNombre.setError("Ingrese el nombre completo");
            campoNombre.requestFocus();
            return;
        }

        // Parámetro 'accion' según sea nuevo registro o modificación
        String accion = esModificacion ? "editar" : "Agregar";
        botonModificar.setEnabled(false);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("accion", accion);
                parametros.put("email", email);
                parametros.put("psw", password);
                parametros.put("nombre", nombre);

                boolean operacionExitosa = false;
                String mensajeResultado = "";

                try {
                    // Petición HTTP a la URL centralizada (192.168.1.11)
                    String respuesta = ConexionHttpPostServer.enviarPost(parametros);

                    if (respuesta == null || respuesta.trim().isEmpty()) {
                        mensajeResultado = "No se recibió respuesta del servidor";
                    } else {
                        // Procesamiento del JSON en segundo plano
                        try {
                            JsonObject jsonObject = JsonParser.parseString(respuesta.trim()).getAsJsonObject();
                            if (jsonObject.has("mensaje")) {
                                String mensajeServidor = jsonObject.get("mensaje").getAsString();
                                if ("OK".equalsIgnoreCase(mensajeServidor)) {
                                    operacionExitosa = true;
                                    mensajeResultado = "Operación realizada con éxito";
                                } else {
                                    mensajeResultado = mensajeServidor;
                                }
                            } else {
                                mensajeResultado = "Respuesta del servidor sin campo 'mensaje'";
                            }
                        } catch (Exception eJson) {
                            if (respuesta.toUpperCase().contains("OK")) {
                                operacionExitosa = true;
                                mensajeResultado = "Operación realizada con éxito";
                            } else {
                                mensajeResultado = "Respuesta no válida del servidor";
                            }
                        }
                    }
                } catch (Exception e) {
                    // Captura de errores de red o excepciones lanzadas por detección de HTML
                    mensajeResultado = e.getMessage() != null ? e.getMessage() : "Error de comunicación con el servidor";
                }

                final boolean exitoFinal = operacionExitosa;
                final String mensajeFinal = mensajeResultado;

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        botonModificar.setEnabled(true);
                        if (exitoFinal) {
                            Toast.makeText(PantallaCrudUsuario.this, mensajeFinal, Toast.LENGTH_SHORT).show();
                            // Regresar a la Pantalla de Inicio
                            Intent intent = new Intent(PantallaCrudUsuario.this, PantallaInicio.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(PantallaCrudUsuario.this, "Error: " + mensajeFinal, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
