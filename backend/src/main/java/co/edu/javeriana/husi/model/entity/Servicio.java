package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "servicio")
@Getter
@Setter
public class Servicio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_servicio")
  private Integer id;

  @Column(nullable = false, length = 150)
  private String nombre;

  @Column(columnDefinition = "text")
  private String descripcion;

  @Column(name = "cupo_simultaneo", nullable = false)
  private Integer cupoSimultaneo;

  @Column(name = "cupo_maximo_instalado", nullable = false)
  private Integer cupoMaximoInstalado;

  @Column(nullable = false, length = 20)
  private String estado = "Activo";
}
