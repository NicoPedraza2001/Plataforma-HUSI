package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.UniversidadRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.mapper.UniversidadMapper;
import co.edu.javeriana.husi.model.entity.Universidad;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
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

  private final UniversidadRepository universidadRepository;
  private final UniversidadMapper universidadMapper;
  private final ConsultaService consultaService;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;

  public List<UniversidadCatalogoResponse> catalogo() {
    return universidadRepository
      .findByEstadoOrderByNombreAsc("Activo")
      .stream()
      .map(universidad ->
        new UniversidadCatalogoResponse(
          universidad.getId(),
          universidad.getNombre(),
          universidad.getPais()
        )
      )
      .toList();
  }

  public UniversidadResponse consultar(Integer id) {
    return universidadMapper.toResponse(buscarUniversidad(id, false));
  }

  private Universidad buscarUniversidad(Integer id, boolean bloquear) {
    if (id == null || id <= 0) throw error(
      HttpStatus.BAD_REQUEST,
      "La universidad indicada no es válida."
    );
    Universidad universidad = (
      bloquear
        ? universidadRepository.bloquearPorId(id)
        : universidadRepository.findById(id)
    ).orElseThrow(() ->
      error(HttpStatus.NOT_FOUND, "No se encontró la universidad.")
    );
    if ("Eliminado".equals(universidad.getEstado())) throw error(
      HttpStatus.NOT_FOUND,
      "La universidad no está disponible."
    );
    return universidad;
  }

  /** Valida la solicitud y copia sus datos sobre la universidad (nueva o existente). */
  private void validarYAsignarDatos(
    Universidad universidad,
    UniversidadRequest request
  ) {
    String nombre = validarTexto(request.nombre(), 150, "nombre");
    String pais = validarTexto(request.pais(), 100, "país");
    if (
      universidadRepository.existsByNombreIgnoreCaseAndIdNot(
        nombre,
        universidad.getId() == null ? 0 : universidad.getId()
      )
    ) throw error(
      HttpStatus.CONFLICT,
      "Ya existe una universidad con ese nombre. Revisa también las inactivas."
    );
    universidad.setNombre(nombre);
    universidad.setPais(pais);
  }

  /** Devuelve el texto obligatorio sin espacios sobrantes o rechaza si está vacío o es muy largo. */
  private String validarTexto(
    String valor,
    int longitudMaxima,
    String nombreCampo
  ) {
    String limpio = valor == null ? "" : valor.strip();
    if (limpio.isEmpty()) throw error(
      HttpStatus.BAD_REQUEST,
      "El campo " + nombreCampo + " es obligatorio."
    );
    if (limpio.length() > longitudMaxima) throw error(
      HttpStatus.BAD_REQUEST,
      "El campo " +
        nombreCampo +
        " admite hasta " +
        longitudMaxima +
        " caracteres."
    );
    return limpio;
  }

  @Transactional
  public UniversidadResponse crear(UniversidadRequest request) {
    Universidad universidad = new Universidad();
    validarYAsignarDatos(universidad, request);
    Universidad universidadCreada = universidadRepository.saveAndFlush(universidad);
    historialAuditoria.registrar(
      AccionAuditoria.CREO,
      ModuloAuditoria.UNIVERSIDADES,
      null,
      fotografias.deUniversidad(universidadCreada)
    );
    return universidadMapper.toResponse(universidadCreada);
  }

  @Transactional
  public UniversidadResponse actualizar(
    Integer id,
    UniversidadRequest request
  ) {
    Universidad universidad = buscarUniversidad(id, true);
    FotografiaRegistro antes = fotografias.deUniversidad(universidad);
    validarYAsignarDatos(universidad, request);
    Universidad universidadActualizada = universidadRepository.saveAndFlush(universidad);
    historialAuditoria.registrar(
      AccionAuditoria.ACTUALIZO,
      ModuloAuditoria.UNIVERSIDADES,
      antes,
      fotografias.deUniversidad(universidadActualizada)
    );
    return universidadMapper.toResponse(universidadActualizada);
  }

  @Transactional
  public UniversidadResponse cambiarEstado(Integer id, String estado) {
    if (
      !Set.of("Activo", "Inactivo").contains(estado == null ? "" : estado)
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "El estado debe ser Activo o Inactivo."
    );
    Universidad universidad = buscarUniversidad(id, true);
    FotografiaRegistro antes = fotografias.deUniversidad(universidad);
    universidad.setEstado(estado);
    // No hay cascada: programas y residentes conservan registros, relaciones y estados.
    Universidad universidadActualizada = universidadRepository.saveAndFlush(universidad);
    historialAuditoria.registrar(
      AccionAuditoria.segunEstado(estado),
      ModuloAuditoria.UNIVERSIDADES,
      antes,
      fotografias.deUniversidad(universidadActualizada)
    );
    return universidadMapper.toResponse(universidadActualizada);
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    Universidad universidad = buscarUniversidad(id, true);
    long programasAsociados = universidadRepository.contarProgramas(id);
    if (programasAsociados > 0) throw error(
      HttpStatus.CONFLICT,
      "No se puede eliminar definitivamente: esta universidad tiene " +
        programasAsociados +
        " programas asociados, incluidos los inactivos. Puedes desactivarla para conservar sus relaciones."
    );
    FotografiaRegistro antes = fotografias.deUniversidad(universidad);
    universidadRepository.delete(universidad);
    universidadRepository.flush();
    historialAuditoria.registrar(
      AccionAuditoria.ELIMINO,
      ModuloAuditoria.UNIVERSIDADES,
      antes,
      null
    );
  }

  public PaginaResponse<Map<String, Object>> programas(
    Integer id,
    Map<String, String> filtros
  ) {
    buscarUniversidad(id, false);
    Map<String, String> parametros = new HashMap<>(filtros);
    // El ID de la ruta tiene precedencia; nunca consultar otra universidad por un filtro recibido.
    parametros.put("universidadId", id.toString());
    return consultaService.listar("programas", parametros);
  }

  private NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }
}
