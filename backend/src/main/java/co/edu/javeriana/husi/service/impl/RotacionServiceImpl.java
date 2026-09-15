package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.dto.response.RotacionResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.RotacionService;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RotacionServiceImpl implements RotacionService {

  private final RotacionRepository repo;
  private final co.edu.javeriana.husi.mapper.RotacionMapper mapper;
  private final RotacionConsultaRepository consultas;

  private NegocioException error(HttpStatus s, String m) {
    return new NegocioException(s, m);
  }

  private int numero(String v, int max) {
    try {
      int n = Integer.parseInt(v);
      if (n < 1 || n > max) throw new Exception();
      return n;
    } catch (Exception e) {
      throw error(
        HttpStatus.BAD_REQUEST,
        "La página, tamaño o servicio indicado no es válido."
      );
    }
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaResponse<RotacionResponse> listar(Map<String, String> filtros) {
    if (
      !Set.of("q", "activa", "servicioId", "pagina", "tamano").containsAll(
        filtros.keySet()
      )
    ) throw error(HttpStatus.BAD_REQUEST, "Filtro de rotaciones desconocido.");
    String q = filtros.getOrDefault("q", "").strip();
    if (q.length() > 200) throw error(
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
    Integer servicio = filtros.containsKey("servicioId")
      ? numero(filtros.get("servicioId"), Integer.MAX_VALUE)
      : null;
    var pagina = consultas.listar(
      q,
      activa,
      servicio,
      numero(filtros.getOrDefault("pagina", "1"), 1000000),
      numero(filtros.getOrDefault("tamano", "10"), 100)
    );
    return new PaginaResponse<>(
      pagina.contenido().stream().map(mapper::toResponse).toList(),
      pagina.totalElementos(),
      pagina.totalPaginas(),
      pagina.pagina(),
      pagina.tamano()
    );
  }

  public Map<String, Object> catalogos() {
    return Map.of(
      "asignaturas",
      consultas.asignaturas(),
      "servicios",
      consultas.servicios()
    );
  }

  private Rotacion obtener(RotacionId id, boolean lock) {
    validar(
      new RotacionRequest(id.getAsignaturaId(), id.getInicio(), id.getFin())
    );
    return (lock ? repo.bloquear(id) : repo.findById(id)).orElseThrow(() ->
      error(
        HttpStatus.NOT_FOUND,
        "No se encontró la rotación. Actualiza el listado."
      )
    );
  }

  private void validar(RotacionRequest d) {
    if (
      d == null ||
      d.asignaturaId() == null ||
      d.asignaturaId() < 1 ||
      d.inicio() == null ||
      d.fin() == null
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "Asignatura y fechas son obligatorias."
    );
    if (d.fin().isBefore(d.inicio())) throw error(
      HttpStatus.BAD_REQUEST,
      "La fecha final no puede ser anterior a la inicial."
    );
    if (d.inicio().getYear() < 1900 || d.fin().getYear() > 9999) throw error(
      HttpStatus.BAD_REQUEST,
      "Las fechas deben estar entre 1900 y 9999."
    );
  }

  private void activa(Integer id) {
    if (!consultas.asignaturaActiva(id)) throw error(
      HttpStatus.BAD_REQUEST,
      "La asignatura no existe o ella o su servicio están inactivos."
    );
  }

  private RotacionId clave(RotacionRequest d) {
    return new RotacionId(d.asignaturaId(), d.inicio(), d.fin());
  }

  public RotacionResponse consultar(RotacionId id) {
    obtener(id, false);
    return mapper.toResponse(consultas.detalle(id));
  }

  @Transactional
  public RotacionResponse crear(RotacionRequest d) {
    validar(d);
    activa(d.asignaturaId());
    RotacionId id = clave(d);
    if (repo.existsById(id)) throw error(
      HttpStatus.CONFLICT,
      "La rotación ya existe para esa asignatura y esas fechas. Revisa también las inactivas."
    );
    Rotacion r = new Rotacion();
    r.setId(id);
    consultas.insertar(r);
    return mapper.toResponse(consultas.detalle(id));
  }

  @Transactional
  public RotacionResponse editar(RotacionId id, RotacionRequest d) {
    obtener(id, true);
    validar(d);
    if (id.equals(clave(d))) return mapper.toResponse(consultas.detalle(id));
    if (consultas.relaciones(id) > 0) throw error(
      HttpStatus.CONFLICT,
      "No se pueden cambiar las fechas de una rotación con residentes relacionados. Conserva su historial o crea otra rotación."
    );
    // La asignatura define la capacidad inicial; no trasladar una rotación entre servicios.
    if (!id.getAsignaturaId().equals(d.asignaturaId())) throw error(
      HttpStatus.BAD_REQUEST,
      "La asignatura no puede cambiarse. Crea otra rotación para esa asignatura."
    );
    if (repo.existsById(clave(d))) throw error(
      HttpStatus.CONFLICT,
      "Ya existe una rotación con esas fechas."
    );
    consultas.cambiarClave(id, d);
    return mapper.toResponse(consultas.detalle(clave(d)));
  }

  @Transactional
  public RotacionResponse estado(RotacionId id, boolean valor) {
    Rotacion r = obtener(id, true);
    if (valor) activa(id.getAsignaturaId());
    r.setActiva(valor);
    repo.saveAndFlush(r);
    return mapper.toResponse(consultas.detalle(id));
  }

  @Transactional
  public void eliminar(RotacionId id) {
    Rotacion r = obtener(id, true);
    long relaciones = consultas.relaciones(id);
    if (relaciones > 0) throw error(
      HttpStatus.CONFLICT,
      "No se puede eliminar: tiene " +
        relaciones +
        " registros de residentes relacionados (incluidos cancelados). Puedes desactivarla para conservar el historial."
    );
    repo.delete(r);
    repo.flush();
  }

  public RotacionRequest validarFila(ImportacionRotacionesRequest.Fila f) {
    String a = f.asignatura() == null ? "" : f.asignatura().strip(),
      s = f.servicio() == null ? "" : f.servicio().strip();
    if (a.isEmpty() || s.isEmpty()) throw error(
      HttpStatus.BAD_REQUEST,
      "Asignatura y servicio son obligatorios."
    );
    var encontrados = consultas.resolver(a, s);
    if (encontrados.size() != 1) throw error(
      HttpStatus.BAD_REQUEST,
      "No se encontró una asignatura activa única con ese nombre y servicio. Deben estar registrados previamente."
    );
    LocalDate inicio, fin;
    try {
      inicio = LocalDate.parse(f.inicio().strip());
      fin = LocalDate.parse(f.fin().strip());
    } catch (Exception e) {
      throw error(
        HttpStatus.BAD_REQUEST,
        "Fechas inválidas. Usa AAAA-MM-DD o celdas de fecha de Excel."
      );
    }
    var d = new RotacionRequest(
      ((Number) encontrados.getFirst().get("id")).intValue(),
      inicio,
      fin
    );
    validar(d);
    if (repo.existsById(clave(d))) throw error(
      HttpStatus.CONFLICT,
      "La rotación ya existe; no se modificará ni reactivará."
    );
    return d;
  }
}
