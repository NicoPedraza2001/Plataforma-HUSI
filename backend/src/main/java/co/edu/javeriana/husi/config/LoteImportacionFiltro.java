package co.edu.javeriana.husi.config;

import co.edu.javeriana.husi.util.ImportacionEnCurso;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * El asistente de importación de Excel crea universidades, programas, residentes… con las
 * mismas rutas que los formularios individuales. Para que el historial los resuma en una sola
 * fila, el asistente envía en cada petición la cabecera {@value #CABECERA_LOTE} con el mismo
 * UUID, y opcionalmente {@value #CABECERA_ARCHIVO} con el nombre del archivo (codificado como URL).
 */
@Component
public class LoteImportacionFiltro extends OncePerRequestFilter {

  public static final String CABECERA_LOTE = "X-Lote-Importacion";
  public static final String CABECERA_ARCHIVO = "X-Archivo-Importacion";
  private static final Pattern FORMATO_UUID = Pattern.compile(
    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
  );
  private static final int LONGITUD_MAXIMA_ARCHIVO = 200;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain cadenaDeFiltros
  ) throws ServletException, IOException {
    String idLote = request.getHeader(CABECERA_LOTE);
    if (idLote == null || !FORMATO_UUID.matcher(idLote).matches()) {
      cadenaDeFiltros.doFilter(request, response);
      return;
    }
    ImportacionEnCurso.iniciar(
      new ImportacionEnCurso.Lote(
        idLote.toLowerCase(),
        ImportacionEnCurso.CARGA_INICIAL,
        leerNombreArchivo(request.getHeader(CABECERA_ARCHIVO))
      )
    );
    try {
      cadenaDeFiltros.doFilter(request, response);
    } finally {
      ImportacionEnCurso.terminar();
    }
  }

  private String leerNombreArchivo(String cabecera) {
    if (cabecera == null || cabecera.isBlank()) return null;
    try {
      String nombre = URLDecoder.decode(cabecera, StandardCharsets.UTF_8).strip();
      return nombre.length() > LONGITUD_MAXIMA_ARCHIVO
        ? nombre.substring(0, LONGITUD_MAXIMA_ARCHIVO)
        : nombre;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
