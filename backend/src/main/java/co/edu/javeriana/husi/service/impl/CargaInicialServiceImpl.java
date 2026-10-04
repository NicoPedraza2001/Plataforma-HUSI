package co.edu.javeriana.husi.service.impl;

import static co.edu.javeriana.husi.service.impl.CargaInicialPlan.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.repository.CargaInicialRepository;
import co.edu.javeriana.husi.service.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CargaInicialServiceImpl implements CargaInicialService {

  private final CargaInicialRepository cargaInicialRepository;
  private final UniversidadService universidadService;
  private final ProgramaService programaService;
  private final ResidenteService residenteService;
  private final ServicioService servicioService;
  private final AsignaturaService asignaturaService;
  private final RotacionService rotacionService;
  private final RotacionResidenteService rotacionResidenteService;

  @Transactional(readOnly = true)
  public CargaInicialResponse validar(CargaInicialRequest solicitud) {
    return simularOGuardar(solicitud, false);
  }

  @Transactional
  public CargaInicialResponse confirmar(CargaInicialRequest solicitud) {
    return simularOGuardar(solicitud, true);
  }

  private CargaInicialResponse simularOGuardar(
    CargaInicialRequest solicitud,
    boolean guardar
  ) {
    if (
      solicitud == null ||
      solicitud.filas() == null ||
      solicitud.filas().isEmpty() ||
      solicitud.filas().size() > 5000
    ) throw error("Selecciona entre 1 y 5000 filas.");
    var tablasActuales = cargaInicialRepository.leerTablas(guardar);
    String huella = calcularHuella(tablasActuales, solicitud);
    if (
      guardar && !huella.equals(solicitud.validacion())
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Los datos o los cupos cambiaron desde la revisión. Vuelve a validar antes de confirmar; no se guardó nada."
    );
    CargaInicialPlan plan;
    try {
      plan = new CargaInicialPlan(tablasActuales, solicitud);
    } catch (IllegalArgumentException e) {
      throw error(e.getMessage());
    }
    if (guardar) {
      if (plan.registrosNuevos.isEmpty()) throw error(
        "No hay filas válidas para importar."
      );
      Map<Integer, Integer> idRealPorTemporal = new HashMap<>();
      for (var registro : plan.registrosNuevos) guardarRegistro(
        registro,
        idRealPorTemporal
      );
    }
    return plan.construirRespuesta(huella, guardar);
  }

  /** Lee un id del registro; si es temporal (negativo) devuelve el id real ya creado. */
  private int idReal(
    Map<String, Object> valores,
    String columna,
    Map<Integer, Integer> idRealPorTemporal
  ) {
    int id = entero(valores.get(columna));
    return idRealPorTemporal.getOrDefault(id, id);
  }

  private void guardarRegistro(
    RegistroNuevo registro,
    Map<Integer, Integer> idRealPorTemporal
  ) {
    var valores = registro.valores();
    Integer idCreado = null;
    switch (registro.tabla()) {
      case "universidad" -> idCreado = universidadService
        .crear(
          new UniversidadRequest(
            texto(valores.get("nombre_universidad")),
            texto(valores.get("pais")),
            null
          )
        )
        .id();
      case "programa" -> idCreado = programaService
        .crear(
          new ProgramaRequest(
            texto(valores.get("nombre_programa")),
            idReal(valores, "id_universidad", idRealPorTemporal)
          )
        )
        .id();
      case "residente" -> idCreado = residenteService
        .crear(
          new ResidenteRequest(
            texto(valores.get("nombre_completo")),
            texto(valores.get("tipo_documento")),
            texto(valores.get("numero_documento")),
            texto(valores.get("perfil_academico")),
            idReal(valores, "id_programa", idRealPorTemporal)
          )
        )
        .id();
      case "servicio" -> idCreado = servicioService
        .crear(
          new ServicioRequest(
            texto(valores.get("nombre")),
            null,
            entero(valores.get("cupo_simultaneo")),
            entero(valores.get("cupo_maximo_instalado"))
          )
        )
        .id();
      case "asignatura" -> idCreado = asignaturaService
        .crear(
          new AsignaturaRequest(
            texto(valores.get("nombre_asignatura")),
            idReal(valores, "id_servicio", idRealPorTemporal)
          )
        )
        .id();
      case "rotacion" -> rotacionService.crearHistorica(
        new RotacionRequest(
          idReal(valores, "id_asignatura", idRealPorTemporal),
          leerFecha(valores, "fecha_inicio"),
          leerFecha(valores, "fecha_fin")
        )
      );
      case "rotacion_residente" -> rotacionResidenteService.asignarHistorica(
        new RotacionId(
          idReal(valores, "id_asignatura", idRealPorTemporal),
          leerFecha(valores, "fecha_inicio"),
          leerFecha(valores, "fecha_fin")
        ),
        idReal(valores, "id_residente", idRealPorTemporal)
      );
      default -> throw new IllegalStateException("Tabla no admitida");
    }
    if (idCreado != null) idRealPorTemporal.put(
      entero(valores.get("id_" + registro.tabla())),
      idCreado
    );
  }

  private LocalDate leerFecha(Map<String, Object> valores, String columna) {
    return LocalDate.parse(texto(valores.get(columna)));
  }

  /**
   * SHA-256 de las tablas y de la solicitud. Confirmar exige la misma huella que devolvió la
   * validación, para no guardar si los datos cambiaron entre ambos pasos.
   */
  private String calcularHuella(
    Map<String, List<Map<String, Object>>> tablas,
    CargaInicialRequest solicitud
  ) {
    try {
      // TreeMap hace estable el orden de columnas en PostgreSQL y H2.
      StringBuilder contenido = new StringBuilder();
      tablas.forEach((tabla, filas) -> {
        contenido.append(tabla);
        filas.forEach(fila -> contenido.append(new TreeMap<>(fila)));
      });
      contenido.append(solicitud.crearFaltantes()).append(solicitud.filas());
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          contenido.toString().getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private NegocioException error(String mensaje) {
    return new NegocioException(HttpStatus.BAD_REQUEST, mensaje);
  }
}
