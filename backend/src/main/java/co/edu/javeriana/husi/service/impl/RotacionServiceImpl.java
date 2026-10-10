package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.dto.response.RotacionResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.mapper.RotacionMapper;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import co.edu.javeriana.husi.service.RotacionService;
import co.edu.javeriana.husi.util.Calendario;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RotacionServiceImpl implements RotacionService {

  private static final Set<String> FILTROS_PERMITIDOS = Set.of(
    "q",
    "activa",
    "servicioId",
    "desde",
    "hasta",
    "pagina",
    "tamano"
  );

  private final RotacionRepository rotacionRepository;
  private final RotacionMapper rotacionMapper;
  private final RotacionConsultaRepository rotacionConsultaRepository;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;

  private NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }

  /** Convierte un parámetro de la URL en un entero entre 1 y {@code maximo}. */
  private int leerEnteroPositivo(String valor, int maximo) {
    try {
      int numero = Integer.parseInt(valor);
      if (numero < 1 || numero > maximo) throw new Exception();
      return numero;
    } catch (Exception e) {
      throw error(
        HttpStatus.BAD_REQUEST,
        "La página, tamaño o servicio indicado no es válido."
      );
    }
  }

  /** Convierte un parámetro de la URL (AAAA-MM-DD) en fecha; null si no viene. */
  private LocalDate leerFechaOpcional(Map<String, String> filtros, String nombreFiltro) {
    String valor = filtros.getOrDefault(nombreFiltro, "").strip();
    if (valor.isEmpty()) return null;
    try {
      return LocalDate.parse(valor);
    } catch (DateTimeParseException e) {
      throw error(
        HttpStatus.BAD_REQUEST,
        "La fecha «" + nombreFiltro + "» no es válida. Usa el formato AAAA-MM-DD."
      );
    }
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaResponse<RotacionResponse> listar(Map<String, String> filtros) {
    if (!FILTROS_PERMITIDOS.containsAll(filtros.keySet())) throw error(
      HttpStatus.BAD_REQUEST,
      "Filtro de rotaciones desconocido."
    );
    String busqueda = filtros.getOrDefault("q", "").strip();
    if (busqueda.length() > 200) throw error(
      HttpStatus.BAD_REQUEST,
      "La búsqueda admite hasta 200 caracteres."
    );
    Boolean activa = null;
    if (filtros.containsKey("activa")) {
      if (!Set.of("true", "false").contains(filtros.get("activa"))) throw error(
        HttpStatus.BAD_REQUEST,
        "El estado no es válido."
      );
      activa = Boolean.valueOf(filtros.get("activa"));
    }
    Integer servicioId = filtros.containsKey("servicioId")
      ? leerEnteroPositivo(filtros.get("servicioId"), Integer.MAX_VALUE)
      : null;
    LocalDate desde = leerFechaOpcional(filtros, "desde");
    LocalDate hasta = leerFechaOpcional(filtros, "hasta");
    if (desde != null && hasta != null && hasta.isBefore(desde)) throw error(
      HttpStatus.BAD_REQUEST,
      "La fecha «hasta» no puede ser anterior a la fecha «desde»."
    );
    var pagina = rotacionConsultaRepository.listar(
      busqueda,
      activa,
      servicioId,
      desde,
      hasta,
      leerEnteroPositivo(filtros.getOrDefault("pagina", "1"), 1000000),
      leerEnteroPositivo(filtros.getOrDefault("tamano", "10"), 100)
    );
    return new PaginaResponse<>(
      pagina.contenido().stream().map(rotacionMapper::toResponse).toList(),
      pagina.totalElementos(),
      pagina.totalPaginas(),
      pagina.pagina(),
      pagina.tamano()
    );
  }

  public Map<String, Object> catalogos() {
    return Map.of(
      "asignaturas",
      rotacionConsultaRepository.asignaturas(),
      "servicios",
      rotacionConsultaRepository.servicios()
    );
  }

  private Rotacion buscarRotacion(RotacionId id, boolean bloquear) {
    validarDatos(
      new RotacionRequest(id.getAsignaturaId(), id.getInicio(), id.getFin())
    );
    return (
      bloquear
        ? rotacionRepository.bloquearPorId(id)
        : rotacionRepository.findById(id)
    ).orElseThrow(() ->
      error(
        HttpStatus.NOT_FOUND,
        "No se encontró la rotación. Actualiza el listado."
      )
    );
  }

  private void validarDatos(RotacionRequest datos) {
    if (
      datos == null ||
      datos.asignaturaId() == null ||
      datos.asignaturaId() < 1 ||
      datos.inicio() == null ||
      datos.fin() == null
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "Asignatura y fechas son obligatorias."
    );
    if (datos.fin().isBefore(datos.inicio())) throw error(
      HttpStatus.BAD_REQUEST,
      "La fecha final no puede ser anterior a la inicial."
    );
    if (
      datos.inicio().getYear() < 1900 || datos.fin().getYear() > 9999
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "Las fechas deben estar entre 1900 y 9999."
    );
  }

  /** Crear o mover fechas: la rotación no puede empezar antes de hoy. */
  private void validarInicioNoPasado(RotacionRequest datos) {
    if (datos.inicio().isBefore(Calendario.hoy())) throw error(
      HttpStatus.BAD_REQUEST,
      "La fecha de inicio no puede ser anterior a hoy (" +
        Calendario.formato(Calendario.hoy()) +
        ")."
    );
  }

  /** Una rotación cuya fecha final ya pasó es de solo lectura. */
  private void validarNoFinalizada(RotacionId id) {
    if (Calendario.finalizada(id.getFin())) throw error(
      HttpStatus.CONFLICT,
      "La rotación finalizó el " +
        Calendario.formato(id.getFin()) +
        " y ya no admite cambios."
    );
  }

  private void validarAsignaturaActiva(Integer asignaturaId) {
    if (!rotacionConsultaRepository.asignaturaActiva(asignaturaId)) throw error(
      HttpStatus.BAD_REQUEST,
      "La asignatura no existe o ella o su servicio están inactivos."
    );
  }

  private RotacionId construirId(RotacionRequest datos) {
    return new RotacionId(datos.asignaturaId(), datos.inicio(), datos.fin());
  }

  public RotacionResponse consultar(RotacionId id) {
    buscarRotacion(id, false);
    return rotacionMapper.toResponse(rotacionConsultaRepository.detalle(id));
  }

  @Transactional
  public RotacionResponse crear(RotacionRequest datos) {
    validarDatos(datos);
    validarInicioNoPasado(datos);
    return guardarNueva(datos);
  }

  @Transactional
  public RotacionResponse crearHistorica(RotacionRequest datos) {
    validarDatos(datos);
    return guardarNueva(datos);
  }

  private RotacionResponse guardarNueva(RotacionRequest datos) {
    validarAsignaturaActiva(datos.asignaturaId());
    RotacionId id = construirId(datos);
    if (rotacionRepository.existsById(id)) throw error(
      HttpStatus.CONFLICT,
      "La rotación ya existe para esa asignatura y esas fechas. Revisa también las inactivas."
    );
    Rotacion rotacion = new Rotacion();
    rotacion.setId(id);
    rotacionConsultaRepository.insertar(rotacion);
    RotacionResponse rotacionCreada = consultarDetalle(id);
    historialAuditoria.registrar(
      AccionAuditoria.CREO,
      ModuloAuditoria.ROTACIONES,
      null,
      fotografias.deRotacion(rotacionCreada)
    );
    return rotacionCreada;
  }

  private RotacionResponse consultarDetalle(RotacionId id) {
    return rotacionMapper.toResponse(rotacionConsultaRepository.detalle(id));
  }

  @Transactional
  public RotacionResponse editar(RotacionId id, RotacionRequest datos) {
    buscarRotacion(id, true);
    validarNoFinalizada(id);
    validarDatos(datos);
    RotacionId nuevoId = construirId(datos);
    if (id.equals(nuevoId)) return rotacionMapper.toResponse(
      rotacionConsultaRepository.detalle(id)
    );
    validarInicioNoPasado(datos);
    if (rotacionConsultaRepository.contarResidentesRelacionados(id) > 0) throw error(
      HttpStatus.CONFLICT,
      "No se pueden cambiar las fechas de una rotación con residentes relacionados. Conserva su historial o crea otra rotación."
    );
    // La asignatura define la capacidad inicial; no trasladar una rotación entre servicios.
    if (!id.getAsignaturaId().equals(datos.asignaturaId())) throw error(
      HttpStatus.BAD_REQUEST,
      "La asignatura no puede cambiarse. Crea otra rotación para esa asignatura."
    );
    if (rotacionRepository.existsById(nuevoId)) throw error(
      HttpStatus.CONFLICT,
      "Ya existe una rotación con esas fechas."
    );
    FotografiaRegistro antes = fotografias.deRotacion(consultarDetalle(id));
    rotacionConsultaRepository.cambiarFechas(id, datos);
    RotacionResponse rotacionActualizada = consultarDetalle(nuevoId);
    historialAuditoria.registrar(
      AccionAuditoria.ACTUALIZO,
      ModuloAuditoria.ROTACIONES,
      antes,
      fotografias.deRotacion(rotacionActualizada)
    );
    return rotacionActualizada;
  }

  @Transactional
  public RotacionResponse cambiarEstado(RotacionId id, boolean activa) {
    Rotacion rotacion = buscarRotacion(id, true);
    validarNoFinalizada(id);
    if (activa) validarAsignaturaActiva(id.getAsignaturaId());
    FotografiaRegistro antes = fotografias.deRotacion(consultarDetalle(id));
    rotacion.setActiva(activa);
    rotacionRepository.saveAndFlush(rotacion);
    RotacionResponse rotacionActualizada = consultarDetalle(id);
    historialAuditoria.registrar(
      activa ? AccionAuditoria.ACTIVO : AccionAuditoria.INACTIVO,
      ModuloAuditoria.ROTACIONES,
      antes,
      fotografias.deRotacion(rotacionActualizada)
    );
    return rotacionActualizada;
  }

  @Transactional
  public void eliminar(RotacionId id) {
    Rotacion rotacion = buscarRotacion(id, true);
    validarNoFinalizada(id);
    long residentesRelacionados =
      rotacionConsultaRepository.contarResidentesRelacionados(id);
    if (residentesRelacionados > 0) throw error(
      HttpStatus.CONFLICT,
      "No se puede eliminar: tiene " +
        residentesRelacionados +
        " registros de residentes relacionados (incluidos cancelados). Puedes desactivarla para conservar el historial."
    );
    FotografiaRegistro antes = fotografias.deRotacion(consultarDetalle(id));
    rotacionRepository.delete(rotacion);
    rotacionRepository.flush();
    historialAuditoria.registrar(AccionAuditoria.ELIMINO, ModuloAuditoria.ROTACIONES, antes, null);
  }

  public RotacionRequest validarFila(ImportacionRotacionesRequest.Fila fila) {
    String nombreAsignatura = fila.asignatura() == null
      ? ""
      : fila.asignatura().strip();
    String nombreServicio = fila.servicio() == null
      ? ""
      : fila.servicio().strip();
    if (nombreAsignatura.isEmpty() || nombreServicio.isEmpty()) throw error(
      HttpStatus.BAD_REQUEST,
      "Asignatura y servicio son obligatorios."
    );
    var asignaturasEncontradas =
      rotacionConsultaRepository.buscarAsignaturasActivas(
        nombreAsignatura,
        nombreServicio
      );
    if (asignaturasEncontradas.size() != 1) throw error(
      HttpStatus.BAD_REQUEST,
      "No se encontró una asignatura activa única con ese nombre y servicio. Deben estar registrados previamente."
    );
    LocalDate inicio, fin;
    try {
      inicio = LocalDate.parse(fila.inicio().strip());
      fin = LocalDate.parse(fila.fin().strip());
    } catch (Exception e) {
      throw error(
        HttpStatus.BAD_REQUEST,
        "Fechas inválidas. Usa AAAA-MM-DD o celdas de fecha de Excel."
      );
    }
    var datos = new RotacionRequest(
      ((Number) asignaturasEncontradas.getFirst().get("id")).intValue(),
      inicio,
      fin
    );
    validarDatos(datos);
    validarInicioNoPasado(datos);
    if (rotacionRepository.existsById(construirId(datos))) throw error(
      HttpStatus.CONFLICT,
      "La rotación ya existe; no se modificará ni reactivará."
    );
    return datos;
  }
}
