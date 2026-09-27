package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rotacion")
@Getter
@Setter
@NoArgsConstructor
public class Rotacion {

  @EmbeddedId
  private RotacionId id;

  @Column(name = "cupo_disponible", insertable = false, updatable = false)
  private Integer cupoDisponible;

  @Column(insertable = false, updatable = false)
  private String estado;

  @Column(nullable = false)
  private boolean activa = true;
}
