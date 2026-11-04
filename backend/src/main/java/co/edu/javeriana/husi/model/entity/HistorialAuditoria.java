package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Un cambio hecho por un usuario. Una importación completa ocupa una sola fila, identificada
 * por {@code loteImportacion}, con el resumen de lo que creó o modificó.
 */
@Entity
@Table(name = "historial_auditoria")
@Getter
@Setter
public class HistorialAuditoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_historial")
  private Integer id;

  /** Cuenta que hizo el cambio; queda en null si después se elimina el usuario. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario")
  private Usuario usuario;

  /** Nombre de quien hizo el cambio, tal como se llamaba en ese momento. */
  @Column(name = "nombre_responsable", nullable = false, length = 201)
  private String nombreResponsable;

  /** Correo de quien hizo el cambio, tal como lo tenía en ese momento. */
  @Column(name = "correo_responsable", nullable = false, length = 150)
  private String correoResponsable;

  @Column(name = "accion_realizada", nullable = false, length = 100)
  private String accion;

  @Column(name = "entidad_modificada", nullable = false, length = 100)
  private String modulo;

  @Column(nullable = false)
  private OffsetDateTime fecha;

  @Column(columnDefinition = "text")
  private String detalle;

  @Column(length = 250)
  private String registro;

  /** JSON {campo: valor} del registro antes del cambio; null al crear. */
  @Column(name = "datos_anteriores", columnDefinition = "text")
  private String datosAnteriores;

  /** JSON {campo: valor} del registro después del cambio; null al eliminar. */
  @Column(name = "datos_nuevos", columnDefinition = "text")
  private String datosNuevos;

  @Column(name = "lote_importacion", length = 36, unique = true)
  private String loteImportacion;
}
