package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "asignatura")
@Getter
@Setter
public class Asignatura {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_asignatura")
  private Integer id;

  @Column(name = "nombre_asignatura", nullable = false, length = 200)
  private String nombre;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_servicio", nullable = false)
  private Servicio servicio;

  @Column
  private String estado = "Activo";
}
