package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.UniversidadRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.UniversidadMapper;
import co.edu.javeriana.husi.model.entity.Universidad;
import co.edu.javeriana.husi.repository.UniversidadRepository;
import co.edu.javeriana.husi.service.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversidadServiceImpl implements UniversidadService {

  private final UniversidadRepository universidades;
  private final UniversidadMapper mapper;
  private final ConsultaService consultas;

  public List<CatalogoResponse> catalogo() {
    return universidades
      .findByEstadoOrderByNombreAsc("Activo")
      .stream()
      .map(u -> new CatalogoResponse(u.getId(), u.getNombre()))
      .toList();
  }

  public UniversidadResponse consultar(Integer id) {
    return mapper.response(obtener(id, false));
  }

  private Universidad obtener(Integer id, boolean bloquear) {
    if (id == null || id <= 0) throw error(
      HttpStatus.BAD_REQUEST,
      "La universidad indicada no es válida."
    );
    Universidad u = (
      bloquear ? universidades.bloquearPorId(id) : universidades.findById(id)
    ).orElseThrow(() ->
      error(HttpStatus.NOT_FOUND, "No se encontró la universidad.")
    );
    if ("Eliminado".equals(u.getEstado())) throw error(
      HttpStatus.NOT_FOUND,
      "La universidad no está disponible."
    );
    return u;
  }

  private void aplicar(Universidad u, UniversidadRequest request) {
    String nombre = texto(request.nombre(), 150, "nombre", true);
    String pais = texto(request.pais(), 100, "país", true);
    String perfil = texto(request.perfil(), 50, "perfil", false);
    if (
      universidades.existsByNombreIgnoreCaseAndIdNot(
        nombre,
        u.getId() == null ? 0 : u.getId()
      )
    ) throw error(
      HttpStatus.CONFLICT,
      "Ya existe una universidad con ese nombre. Revisa también las inactivas."
    );
    u.setNombre(nombre);
    u.setPais(pais);
    u.setPerfil(perfil);
  }

  private String texto(String valor, int max, String campo, boolean requerido) {
    String limpio = valor == null ? "" : valor.strip();
    if (requerido && limpio.isEmpty()) throw error(
      HttpStatus.BAD_REQUEST,
      "El campo " + campo + " es obligatorio."
    );
    if (limpio.length() > max) throw error(
      HttpStatus.BAD_REQUEST,
      "El campo " + campo + " admite hasta " + max + " caracteres."
    );
    return limpio.isEmpty() ? null : limpio;
  }

  @Transactional
  public UniversidadResponse crear(UniversidadRequest request) {
    Universidad u = new Universidad();
    aplicar(u, request);
    return mapper.response(universidades.saveAndFlush(u));
  }

  @Transactional
  public UniversidadResponse actualizar(
    Integer id,
    UniversidadRequest request
  ) {
    Universidad u = obtener(id, true);
    aplicar(u, request);
    return mapper.response(universidades.saveAndFlush(u));
  }

  @Transactional
  public UniversidadResponse cambiarEstado(Integer id, String estado) {
    if (
      !Set.of("Activo", "Inactivo").contains(estado == null ? "" : estado)
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "El estado debe ser Activo o Inactivo."
    );
    Universidad u = obtener(id, true);
    u.setEstado(estado);
    // No hay cascada: programas y residentes conservan registros, relaciones y estados.
    return mapper.response(universidades.saveAndFlush(u));
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    Universidad u = obtener(id, true);
    long cantidad = universidades.contarProgramas(id);
    if (cantidad > 0) throw error(
      HttpStatus.CONFLICT,
      "No se puede eliminar definitivamente: esta universidad tiene " +
        cantidad +
        " programas asociados, incluidos los inactivos. Puedes desactivarla para conservar sus relaciones."
    );
    universidades.delete(u);
    universidades.flush();
  }

  public PaginaResponse<Map<String, Object>> programas(
    Integer id,
    Map<String, String> filtros
  ) {
    obtener(id, false);
    Map<String, String> parametros = new HashMap<>(filtros);
    // El ID de la ruta tiene precedencia; nunca consultar otra universidad por un filtro recibido.
    parametros.put("universidadId", id.toString());
    return consultas.listar("programas", parametros);
  }

  private NegocioException error(HttpStatus status, String mensaje) {
    return new NegocioException(status, mensaje);
  }
}
