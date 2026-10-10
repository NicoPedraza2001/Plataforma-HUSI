package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.service.*;
import co.edu.javeriana.husi.util.ImportacionEnCurso;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImportacionProgramasServiceImpl
  implements ImportacionProgramasService
{

  private final ProgramaService programaService;
  private final HistorialAuditoriaService historialAuditoria;

  public ImportacionProgramasResponse procesar(
    ImportacionProgramasRequest request,
    boolean guardar
  ) {
    if (!guardar) return procesarFilas(request, false);
    // En el historial queda una sola fila con el resumen, no una por programa creado.
    return historialAuditoria.ejecutarComoImportacion(
      ImportacionEnCurso.IMPORTACION_PROGRAMAS,
      () -> {
        var resultado = procesarFilas(request, true);
        historialAuditoria.agregarResultadoImportacion(resumirParaHistorial(resultado));
        return resultado;
      }
    );
  }

  // Deliberadamente sin transacción de lote: cada crear confirma o revierte su propia fila.
  private ImportacionProgramasResponse procesarFilas(
    ImportacionProgramasRequest request,
    boolean guardar
  ) {
    Set<String> programasYaLeidos = new HashSet<>();
    Set<Integer> numerosDeFila = new HashSet<>();
    for (var fila : request.filas()) {
      if (!numerosDeFila.add(fila.fila())) throw new NegocioException(
        HttpStatus.BAD_REQUEST,
        "Cada registro debe indicar una fila de Excel diferente."
      );
    }
    List<ImportacionProgramasResponse.Fila> resultados = new ArrayList<>();
    for (var fila : request.filas()) {
      String estado = "VALIDO",
        mensaje = "Información completa. Se creará un programa.";
      Integer idCreado = null;
      try {
        ProgramaRequest programa = programaService.validarImportacion(
          fila.nombre(),
          fila.universidad()
        );
        String claveUnica =
          programa.universidadId() +
          "|" +
          programa.nombre().toLowerCase(Locale.ROOT);
        if (!programasYaLeidos.add(claveUnica)) {
          estado = "OMITIDO";
          mensaje = "Programa repetido dentro del archivo.";
        } else if (guardar) {
          idCreado = programaService.crear(programa).id();
          estado = "CREADO";
          mensaje = "Programa guardado correctamente.";
        }
      } catch (NegocioException e) {
        estado = e.getStatus() == HttpStatus.CONFLICT ? "OMITIDO" : "ERROR";
        mensaje = e.getMessage();
      } catch (DataAccessException e) {
        estado = "ERROR";
        mensaje =
          "No se pudo guardar o consultar esta fila. Actualiza y vuelve a validar el archivo.";
      }
      resultados.add(
        new ImportacionProgramasResponse.Fila(
          fila.fila(),
          fila.nombre(),
          fila.universidad(),
          estado,
          mensaje,
          idCreado
        )
      );
    }
    return ImportacionProgramasResponse.de(resultados);
  }

  /** Cifras finales que se agregan a la fila resumen de la importación en el historial. */
  private Map<String, Object> resumirParaHistorial(ImportacionProgramasResponse resultado) {
    Map<String, Object> resumen = new LinkedHashMap<>();
    resumen.put("Filas del archivo", resultado.total());
    resumen.put("Filas omitidas", resultado.omitidos());
    resumen.put("Filas con error", resultado.errores());
    resumen.put(HistorialAuditoriaServiceImpl.CLAVE_ESTADO_IMPORTACION, "Finalizada");
    return resumen;
  }
}
