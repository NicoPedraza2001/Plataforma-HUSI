package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.ImportacionRotacionesRequest;
import co.edu.javeriana.husi.dto.response.ImportacionRotacionesResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import co.edu.javeriana.husi.service.ImportacionRotacionesService;
import co.edu.javeriana.husi.service.RotacionService;
import co.edu.javeriana.husi.util.ImportacionEnCurso;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImportacionRotacionesServiceImpl
  implements ImportacionRotacionesService
{

  private final RotacionService rotacionService;
  private final HistorialAuditoriaService historialAuditoria;

  public ImportacionRotacionesResponse procesar(
    ImportacionRotacionesRequest request,
    boolean guardar
  ) {
    if (!guardar) return procesarFilas(request, false);
    // En el historial queda una sola fila con el resumen, no una por rotación creada.
    return historialAuditoria.ejecutarComoImportacion(
      ImportacionEnCurso.IMPORTACION_ROTACIONES,
      () -> {
        var resultado = procesarFilas(request, true);
        historialAuditoria.agregarResultadoImportacion(resumirParaHistorial(resultado));
        return resultado;
      }
    );
  }

  // Cada creación usa su propia transacción; validar nunca escribe.
  private ImportacionRotacionesResponse procesarFilas(
    ImportacionRotacionesRequest request,
    boolean guardar
  ) {
    if (
      request == null ||
      request.filas() == null ||
      request.filas().isEmpty() ||
      request.filas().size() > 1000
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Selecciona entre 1 y 1000 filas."
    );
    Set<Integer> numerosDeFila = new HashSet<>();
    for (var fila : request.filas())
      if (
        fila == null ||
        fila.fila() == null ||
        fila.fila() < 1 ||
        !numerosDeFila.add(fila.fila())
      ) throw new NegocioException(
        HttpStatus.BAD_REQUEST,
        "Los números de fila deben ser positivos y distintos."
      );
    List<ImportacionRotacionesResponse.Fila> resultados = new ArrayList<>();
    Set<String> rotacionesYaLeidas = new HashSet<>();
    for (var fila : request.filas()) {
      String estado = "VALIDO",
        mensaje = "Se creará una rotación activa.";
      try {
        var rotacion = rotacionService.validarFila(fila);
        if (!rotacionesYaLeidas.add(rotacion.toString())) {
          estado = "OMITIDO";
          mensaje = "Rotación repetida dentro del archivo.";
        } else if (guardar) {
          rotacionService.crear(rotacion);
          estado = "CREADO";
          mensaje = "Rotación guardada.";
        }
      } catch (NegocioException e) {
        estado = e.getStatus() == HttpStatus.CONFLICT ? "OMITIDO" : "ERROR";
        mensaje = e.getMessage();
      } catch (DataAccessException e) {
        estado = "ERROR";
        mensaje =
          "No se pudo guardar o consultar la fila. Vuelve a validar; las rotaciones existentes se omitirán.";
      }
      resultados.add(
        new ImportacionRotacionesResponse.Fila(fila.fila(), estado, mensaje)
      );
    }
    return new ImportacionRotacionesResponse(
      resultados.size(),
      contarPorEstado(resultados, "VALIDO"),
      contarPorEstado(resultados, "CREADO"),
      contarPorEstado(resultados, "OMITIDO"),
      contarPorEstado(resultados, "ERROR"),
      resultados
    );
  }

  /** Cifras finales que se agregan a la fila resumen de la importación en el historial. */
  private Map<String, Object> resumirParaHistorial(ImportacionRotacionesResponse resultado) {
    Map<String, Object> resumen = new LinkedHashMap<>();
    resumen.put("Filas del archivo", resultado.total());
    resumen.put("Filas omitidas", resultado.omitidos());
    resumen.put("Filas con error", resultado.errores());
    resumen.put(HistorialAuditoriaServiceImpl.CLAVE_ESTADO_IMPORTACION, "Finalizada");
    return resumen;
  }

  private long contarPorEstado(
    List<ImportacionRotacionesResponse.Fila> resultados,
    String estado
  ) {
    return resultados
      .stream()
      .filter(resultado -> estado.equals(resultado.estado()))
      .count();
  }
}
