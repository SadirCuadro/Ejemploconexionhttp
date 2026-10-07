package johnarrieta.electiva.ejemploconexionhttp.controladores;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConexionHttpPostServer {

    private static final String TAG = "DEBUG_HTTP";

    // URL base centralizada con la IP del servidor local en la red Wi-Fi
    public static String URL_SERVIDOR = "http://192.168.1.11/crudphpjson/crud/operacion.php";

    public static String enviarPost(Map<String, String> parametros) throws Exception {
        return enviarPost(URL_SERVIDOR, parametros);
    }

    public static String enviarPost(String urlServidor, Map<String, String> parametros) throws Exception {
        HttpURLConnection conexion = null;
        BufferedReader lector = null;
        StringBuilder respuesta = new StringBuilder();

        try {
            URL url = new URL(urlServidor);
            conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("POST");
            conexion.setDoOutput(true);
            conexion.setDoInput(true);
            conexion.setConnectTimeout(15000);
            conexion.setReadTimeout(15000);
            conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            // Codificar parámetros clave-valor (ej: accion, email, psw, nombre)
            StringBuilder postData = new StringBuilder();
            if (parametros != null && !parametros.isEmpty()) {
                for (Map.Entry<String, String> entry : parametros.entrySet()) {
                    if (postData.length() != 0) {
                        postData.append('&');
                    }
                    postData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(entry.getValue() != null ? entry.getValue() : "", "UTF-8"));
                }
            }

            // Enviar datos en el flujo de salida
            byte[] datosBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
            conexion.setRequestProperty("Content-Length", String.valueOf(datosBytes.length));

            try (OutputStream flujoSalida = conexion.getOutputStream()) {
                flujoSalida.write(datosBytes);
                flujoSalida.flush();
            }

            // Validar código de respuesta HTTP
            int codigoRespuesta = conexion.getResponseCode();
            InputStream flujoEntrada;
            if (codigoRespuesta >= 200 && codigoRespuesta < 400) {
                flujoEntrada = conexion.getInputStream();
            } else {
                flujoEntrada = conexion.getErrorStream();
            }

            if (flujoEntrada != null) {
                // Detección y lectura robusta con charset UTF-8 o fallback ISO-8859-1
                Charset charset = StandardCharsets.UTF_8;
                String contentType = conexion.getContentType();
                if (contentType != null && contentType.toLowerCase().contains("iso-8859-1")) {
                    charset = Charset.forName("ISO-8859-1");
                }

                lector = new BufferedReader(new InputStreamReader(flujoEntrada, charset));
                String linea;
                while ((linea = lector.readLine()) != null) {
                    respuesta.append(linea);
                }
            }
        } finally {
            if (lector != null) {
                try {
                    lector.close();
                } catch (Exception ignored) {
                }
            }
            if (conexion != null) {
                conexion.disconnect();
            }
        }

        String respuestaFinal = respuesta.toString();

        // Imprimir respuesta cruda para depuración
        Log.d(TAG, "Respuesta: " + respuestaFinal);

        // Validación previa: detectar si el servidor devolvió una página HTML en lugar de JSON
        String textoLimpio = respuestaFinal.trim().toLowerCase();
        if (textoLimpio.startsWith("<!doctype") || textoLimpio.startsWith("<html")) {
            throw new Exception("Error del servidor: URL incorrecta o script PHP falló");
        }

        return respuestaFinal;
    }
}
