package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
  name = "programa",
  uniqueConstraints = @UniqueConstraint(
    columnNames = { "id_universidad", "nombre_programa" }
  )
)
@Getter
@Setter
public class Programa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_programa")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_universidad", nullable = false)
  private Universidad universidad;

  @Column(name = "nombre_programa", nullable = false, length = 150)
  private String nombre;

  @Column(nullable = false, length = 20)
  private String estado = "Activo";
}
