package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.response.HistorialDetalleResponse;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import java.util.Map;
import java.util.function.Supplier;

public interface HistorialAuditoriaService {
  /**
   * Guarda un cambio en el historial dentro de la transacción del cambio: si este se revierte,
   * su registro en el historial también.
   *
   * @param antes fotografía antes del cambio; null al crear
   * @param despues fotografía después del cambio; null al eliminar
   */
  void registrar(
    AccionAuditoria accion,
    ModuloAuditoria modulo,
    FotografiaRegistro antes,
    FotografiaRegistro despues
  );

  /**
   * Ejecuta una importación de modo que todos sus cambios queden en una sola fila resumen. Si la
   * petición ya pertenece a un lote (asistente de Excel), se suma a ese lote.
   */
  <T> T ejecutarComoImportacion(String descripcion, Supplier<T> importacion);

  /** Agrega datos finales (filas guardadas, omitidas…) al resumen de la importación en curso. */
  void agregarResultadoImportacion(Map<String, Object> resultado);

  /**
   * El administrador abandonó el asistente de Excel antes de confirmar: el resumen queda como
   * «Cancelada» y conserva lo que se alcanzó a crear. No hace nada si el lote no creó nada o
   * ya terminó.
   */
  void cancelarImportacion(String idLote);

  HistorialDetalleResponse consultar(Integer id);
}
