package co.edu.javeriana.husi.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Detalle de un movimiento del historial: quién, cuándo, qué hizo y cómo quedó el registro.
 * En una importación, {@code datosNuevos} trae el resumen (cantidades por módulo) y
 * {@code datosAnteriores} no se envía.
 */
public record HistorialDetalleResponse(
  Integer id,
  OffsetDateTime fecha,
  String usuario,
  String correoUsuario,
  String modulo,
  String accion,
  String registro,
  String detalle,
  Map<String, Object> datosAnteriores,
  Map<String, Object> datosNuevos,
  boolean esImportacion
) {}
