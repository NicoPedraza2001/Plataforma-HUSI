package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.ProgramaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.mapper.ProgramaMapper;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import co.edu.javeriana.husi.service.ProgramaService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramaServiceImpl implements ProgramaService {

  private final ProgramaRepository programaRepository;
  private final UniversidadRepository universidadRepository;
  private final ProgramaMapper programaMapper;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;

  public List<ProgramaResponse> listar() {
    return programaRepository
      .findByEstadoOrderByNombreAscIdAsc("Activo")
      .stream()
      .map(programaMapper::toResponse)
      .toList();
  }

  public ProgramaResponse consultar(Integer id) {
    return programaMapper.toResponse(buscarPrograma(id));
  }

  private Programa buscarPrograma(Integer id) {
    return programaRepository
      .findById(id)
      .orElseThrow(() ->
        error(HttpStatus.NOT_FOUND, "No se encontró el programa.")
      );
  }

  private Programa buscarProgramaBloqueado(Integer id) {
    return programaRepository
      .bloquearPorId(id)
      .orElseThrow(() ->
        error(HttpStatus.NOT_FOUND, "No se encontró el programa.")
      );
  }

  /** Devuelve el nombre sin espacios sobrantes o rechaza si está vacío o es muy largo. */
  private String validarNombre(String nombre) {
    if (nombre == null || nombre.isBlank()) throw error(
      HttpStatus.BAD_REQUEST,
      "Falta el nombre del programa."
    );
    String nombreLimpio = nombre.strip();
    if (nombreLimpio.length() > 150) throw error(
      HttpStatus.BAD_REQUEST,
      "El nombre admite hasta 150 caracteres."
    );
    return nombreLimpio;
  }

  private Universidad buscarUniversidadActiva(Integer universidadId) {
    if (universidadId == null) throw error(
      HttpStatus.BAD_REQUEST,
      "Selecciona una universidad."
    );
    Universidad universidad = universidadRepository
      .bloquearPorId(universidadId)
      .orElseThrow(() ->
        error(HttpStatus.BAD_REQUEST, "La universidad no existe.")
      );
    if (!"Activo".equals(universidad.getEstado())) throw error(
      HttpStatus.BAD_REQUEST,
      "La universidad está inactiva."
    );
    return universidad;
  }

  /**
   * Las universidades externas a la Javeriana no registran programas propios (ver
   * {@link ProgramasDeConvenio}) y el convenio debe corresponder al país de la universidad.
   * Devuelve el nombre tal como se guarda o rechaza cualquier otro.
   */
  private String validarNombreSegunUniversidad(String nombre, Universidad universidad) {
    if (ProgramasDeConvenio.esUniversidadPropia(universidad.getNombre())) return nombre;
    String convenio = ProgramasDeConvenio.buscarConvenio(nombre).orElseThrow(() ->
      error(HttpStatus.BAD_REQUEST, ProgramasDeConvenio.MENSAJE_SOLO_CONVENIOS)
    );
    ProgramasDeConvenio.buscarErrorDeConvenioSegunPais(
      convenio,
      universidad.getNombre(),
      universidad.getPais()
    ).ifPresent(mensaje -> {
      throw error(HttpStatus.BAD_REQUEST, mensaje);
    });
    return convenio;
  }

  /** Rechaza si la universidad ya tiene un programa con ese nombre (salvo {@code idExcluido}). */
  private void validarSinDuplicado(
    Integer universidadId,
    String nombre,
    Integer idExcluido
  ) {
    programaRepository
      .findFirstByUniversidadIdAndNombreIgnoreCase(universidadId, nombre)
      .ifPresent(existente -> {
        if (!Objects.equals(existente.getId(), idExcluido)) throw error(
          HttpStatus.CONFLICT,
          "Ya existe este programa en la universidad (estado: " +
            existente.getEstado() +
            "). No se creará otro."
        );
      });
  }

  @Transactional
  public ProgramaResponse crear(ProgramaRequest request) {
    Universidad universidad = buscarUniversidadActiva(request.universidadId());
    String nombre = validarNombreSegunUniversidad(
      validarNombre(request.nombre()),
      universidad
    );
    validarSinDuplicado(universidad.getId(), nombre, null);
    Programa programa = new Programa();
    programa.setNombre(nombre);
    programa.setUniversidad(universidad);
    Programa programaCreado = programaRepository.saveAndFlush(programa);
    historialAuditoria.registrar(
      AccionAuditoria.CREO,
      ModuloAuditoria.PROGRAMAS,
      null,
      fotografias.dePrograma(programaCreado)
    );
    return programaMapper.toResponse(programaCreado);
  }

  @Transactional
  public ProgramaResponse actualizar(Integer id, ProgramaRequest request) {
    Programa programa = buscarProgramaBloqueado(id);
    if ("Eliminado".equals(programa.getEstado())) throw error(
      HttpStatus.CONFLICT,
      "El programa no está disponible."
    );
    Universidad universidad = buscarUniversidadActiva(request.universidadId());
    String nombre = validarNombreSegunUniversidad(
      validarNombre(request.nombre()),
      universidad
    );
    validarSinDuplicado(universidad.getId(), nombre, id);
    FotografiaRegistro antes = fotografias.dePrograma(programa);
    programa.setNombre(nombre);
    programa.setUniversidad(universidad);
    Programa programaActualizado = programaRepository.saveAndFlush(programa);
    historialAuditoria.registrar(
      AccionAuditoria.ACTUALIZO,
      ModuloAuditoria.PROGRAMAS,
      antes,
      fotografias.dePrograma(programaActualizado)
    );
    return programaMapper.toResponse(programaActualizado);
  }

  @Transactional
  public void desactivar(Integer id) {
    Programa programa = buscarProgramaBloqueado(id);
    FotografiaRegistro antes = fotografias.dePrograma(programa);
    programa.setEstado("Inactivo");
    programaRepository.saveAndFlush(programa);
    historialAuditoria.registrar(
      AccionAuditoria.INACTIVO,
      ModuloAuditoria.PROGRAMAS,
      antes,
      fotografias.dePrograma(programa)
    );
  }

  @Transactional
  public ProgramaResponse cambiarEstado(Integer id, String estado) {
    if (
      !Set.of("Activo", "Inactivo").contains(estado == null ? "" : estado)
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "El estado debe ser Activo o Inactivo."
    );
    Programa programa = buscarProgramaBloqueado(id);
    if ("Eliminado".equals(programa.getEstado())) throw error(
      HttpStatus.CONFLICT,
      "El programa no está disponible."
    );
    if ("Activo".equals(estado)) buscarUniversidadActiva(
      programa.getUniversidad().getId()
    );
    FotografiaRegistro antes = fotografias.dePrograma(programa);
    programa.setEstado(estado);
    Programa programaActualizado = programaRepository.saveAndFlush(programa);
    historialAuditoria.registrar(
      AccionAuditoria.segunEstado(estado),
      ModuloAuditoria.PROGRAMAS,
      antes,
      fotografias.dePrograma(programaActualizado)
    );
    return programaMapper.toResponse(programaActualizado);
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    Programa programa = buscarProgramaBloqueado(id);
    long residentesAsociados = programaRepository.contarResidentes(id);
    if (residentesAsociados > 0) throw error(
      HttpStatus.CONFLICT,
      "No se puede eliminar definitivamente: este programa tiene " +
        residentesAsociados +
        " residentes asociados, incluidos los inactivos. Puedes desactivarlo para conservar su historial."
    );
    FotografiaRegistro antes = fotografias.dePrograma(programa);
    programaRepository.delete(programa);
    programaRepository.flush();
    historialAuditoria.registrar(AccionAuditoria.ELIMINO, ModuloAuditoria.PROGRAMAS, antes, null);
  }

  public ProgramaRequest validarImportacion(
    String nombre,
    String nombreUniversidad
  ) {
    String nombreLimpio = validarNombre(nombre);
    if (nombreUniversidad == null || nombreUniversidad.isBlank()) throw error(
      HttpStatus.BAD_REQUEST,
      "Falta la universidad."
    );
    if (nombreUniversidad.strip().length() > 150) throw error(
      HttpStatus.BAD_REQUEST,
      "La universidad admite hasta 150 caracteres."
    );
    List<Universidad> coincidencias =
      universidadRepository.findByNombreIgnoreCaseAndEstado(
        nombreUniversidad.strip(),
        "Activo"
      );
    if (coincidencias.isEmpty()) throw error(
      HttpStatus.BAD_REQUEST,
      "Universidad no encontrada o inactiva. Usa el nombre del catálogo."
    );
    if (coincidencias.size() != 1) throw error(
      HttpStatus.BAD_REQUEST,
      "El nombre de universidad es ambiguo. Revisa el catálogo."
    );
    Universidad universidad = coincidencias.getFirst();
    String nombrePrograma = validarNombreSegunUniversidad(nombreLimpio, universidad);
    validarSinDuplicado(universidad.getId(), nombrePrograma, null);
    return new ProgramaRequest(nombrePrograma, universidad.getId());
  }

  private static NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }
}
