package johnarrieta.electiva.ejemploconexionhttp.vistas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import johnarrieta.electiva.ejemploconexionhttp.R;
import johnarrieta.electiva.ejemploconexionhttp.controladores.ConexionHttpPostServer;
import johnarrieta.electiva.ejemploconexionhttp.datos.Usuario;

public class PantallaListado extends AppCompatActivity {

    private TextView etiTitulo3;
    private ListView listaUsuarios;
    private Button btnGuardar;

    private Usuario usuarioActivo;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pantalla_listado);

        etiTitulo3 = findViewById(R.id.etiTitulo3);
        listaUsuarios = findViewById(R.id.listaUsuarios);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Recuperar usuario activo desde el Intent o desde SharedPreferences
        usuarioActivo = (Usuario) getIntent().getSerializableExtra("usuario");
        if (usuarioActivo == null) {
            SharedPreferences preferences = getSharedPreferences("sesion_usuario", MODE_PRIVATE);
            String email = preferences.getString("email", "");
            String psw = preferences.getString("password", "");
            String nombre = preferences.getString("nombre", "");
            if (!email.isEmpty()) {
                usuarioActivo = new Usuario(email, psw, nombre);
            }
        }

        // Botón para editar datos del usuario activo
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PantallaListado.this, PantallaCrudUsuario.class);
                intent.putExtra("usuario", usuarioActivo);
                startActivity(intent);
            }
        });

        // Cargar lista de usuarios desde el backend
        cargarListaUsuarios();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar la lista por si hubo cambios o inserciones
        cargarListaUsuarios();
    }

    private void cargarListaUsuarios() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("accion", "listar");

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
                        procesarRespuestaListado(respuestaFinal);
                    }
                });
            }
        });
    }

    private void procesarRespuestaListado(String respuestaJson) {
        if (respuestaJson == null || respuestaJson.trim().isEmpty()) {
            Toast.makeText(this, "Error de conexión al obtener usuarios", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Type tipoLista = new TypeToken<List<Usuario>>() {}.getType();
            List<Usuario> lista = gson.fromJson(respuestaJson.trim(), tipoLista);

            if (lista != null && !lista.isEmpty()) {
                List<String> items = new ArrayList<>();
                for (Usuario u : lista) {
                    items.add(u.getEmail() + " - " + u.getNombre());
                }

                ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                        PantallaListado.this,
                        android.R.layout.simple_list_item_1,
                        items
                );
                listaUsuarios.setAdapter(adaptador);
            } else {
                Toast.makeText(this, "No se encontraron usuarios en la base de datos", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar listado JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
