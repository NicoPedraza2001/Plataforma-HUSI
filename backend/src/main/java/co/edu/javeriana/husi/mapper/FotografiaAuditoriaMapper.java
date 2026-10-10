package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.RotacionResponse;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.repository.ProgramaRepository;
import co.edu.javeriana.husi.repository.RolRepository;
import co.edu.javeriana.husi.util.Calendario;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Convierte cada entidad en la {@link FotografiaRegistro} que guarda el historial: nombres de
 * campo legibles y valores ya resueltos (nombre del programa en vez de su id, fechas en
 * dd/MM/yyyy…), para que el detalle se entienda aunque el registro cambie o se elimine después.
 *
 * <p>Debe llamarse antes de modificar la entidad para obtener los datos anteriores.
 */
@Component
@RequiredArgsConstructor
public class FotografiaAuditoriaMapper {

  private static final String SIN_VALOR = "—";

  private final ProgramaRepository programaRepository;
  private final RolRepository rolRepository;

  public FotografiaRegistro deUniversidad(Universidad universidad) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Nombre", universidad.getNombre());
    datos.put("País", universidad.getPais());
    datos.put("Estado", universidad.getEstado());
    return new FotografiaRegistro(universidad.getNombre(), datos);
  }

  public FotografiaRegistro dePrograma(Programa programa) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Nombre", programa.getNombre());
    datos.put("Universidad", programa.getUniversidad().getNombre());
    datos.put("Estado", programa.getEstado());
    return new FotografiaRegistro(
      programa.getNombre() + " — " + programa.getUniversidad().getNombre(),
      datos
    );
  }

  public FotografiaRegistro deResidente(Residente residente) {
    String documento =
      residente.getTipoDocumento().getCodigo() + " " + residente.getNumeroDocumento();
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Nombre completo", residente.getNombre());
    datos.put("Tipo de documento", residente.getTipoDocumento().getNombre());
    datos.put("Número de documento", residente.getNumeroDocumento());
    datos.put(
      "Perfil académico",
      residente.getPerfilAcademico() == null
        ? SIN_VALOR
        : residente.getPerfilAcademico().getNombre()
    );
    programaRepository
      .findById(residente.getProgramaId())
      .ifPresent(programa -> {
        datos.put("Programa", programa.getNombre());
        datos.put("Universidad", programa.getUniversidad().getNombre());
      });
    datos.put("Estado", residente.getEstado());
    return new FotografiaRegistro(residente.getNombre() + " (" + documento + ")", datos);
  }

  public FotografiaRegistro deServicio(Servicio servicio) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Nombre", servicio.getNombre());
    datos.put(
      "Descripción",
      servicio.getDescripcion() == null ? SIN_VALOR : servicio.getDescripcion()
    );
    datos.put("Cupo simultáneo", servicio.getCupoSimultaneo());
    datos.put("Cupo máximo instalado", servicio.getCupoMaximoInstalado());
    datos.put("Estado", servicio.getEstado());
    return new FotografiaRegistro(servicio.getNombre(), datos);
  }

  public FotografiaRegistro deAsignatura(Asignatura asignatura) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Nombre", asignatura.getNombre());
    datos.put("Servicio", asignatura.getServicio().getNombre());
    datos.put("Estado", asignatura.getEstado());
    return new FotografiaRegistro(
      asignatura.getNombre() + " — " + asignatura.getServicio().getNombre(),
      datos
    );
  }

  public FotografiaRegistro deCoordinador(Coordinador coordinador) {
    Usuario usuario = coordinador.getUsuario();
    String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Usuario", nombreCompleto);
    datos.put("Correo", usuario.getCorreo());
    datos.put("Servicio", coordinador.getServicio().getNombre());
    datos.put("Estado", coordinador.getEstado());
    return new FotografiaRegistro(
      nombreCompleto + " — " + coordinador.getServicio().getNombre(),
      datos
    );
  }

  /** Nunca incluye la contraseña, ni siquiera cifrada. */
  public FotografiaRegistro deUsuario(Usuario usuario) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Nombre", usuario.getNombre());
    datos.put("Apellido", usuario.getApellido());
    datos.put("Correo", usuario.getCorreo());
    datos.put("Rol", rolRepository.consultarNombre(usuario.getRolId()));
    datos.put("Estado", usuario.getEstado());
    return new FotografiaRegistro(
      usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getCorreo() + ")",
      datos
    );
  }

  public FotografiaRegistro deRotacion(RotacionResponse rotacion) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Asignatura", rotacion.asignatura());
    datos.put("Servicio", rotacion.servicio());
    datos.put("Fecha de inicio", formatearFecha(rotacion.inicio()));
    datos.put("Fecha de fin", formatearFecha(rotacion.fin()));
    datos.put("Estado", rotacion.activa() ? "Activa" : "Inactiva");
    datos.put("Cupo disponible", rotacion.cupoDisponible());
    return new FotografiaRegistro(describirRotacion(rotacion), datos);
  }

  /** Relación de un residente con una rotación (tabla rotacion_residente). */
  public FotografiaRegistro deAsignacion(
    Residente residente,
    RotacionResponse rotacion,
    String estadoAsignacion
  ) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("Residente", residente.getNombre());
    datos.put(
      "Documento",
      residente.getTipoDocumento().getCodigo() + " " + residente.getNumeroDocumento()
    );
    datos.put("Asignatura", rotacion.asignatura());
    datos.put("Servicio", rotacion.servicio());
    datos.put("Fecha de inicio", formatearFecha(rotacion.inicio()));
    datos.put("Fecha de fin", formatearFecha(rotacion.fin()));
    datos.put("Estado de la asignación", estadoAsignacion);
    return new FotografiaRegistro(
      residente.getNombre() + " → " + describirRotacion(rotacion),
      datos
    );
  }

  private String describirRotacion(RotacionResponse rotacion) {
    return (
      rotacion.asignatura() +
      " (" +
      formatearFecha(rotacion.inicio()) +
      " – " +
      formatearFecha(rotacion.fin()) +
      ")"
    );
  }

  private String formatearFecha(String fechaIso) {
    return Calendario.formato(LocalDate.parse(fechaIso));
  }
}
