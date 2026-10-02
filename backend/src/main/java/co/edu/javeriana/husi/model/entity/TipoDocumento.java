package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipo_documento")
@Getter
@Setter
public class TipoDocumento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_tipo_documento")
  private Integer id;

  @Column(nullable = false, length = 10, unique = true)
  private String codigo;

  @Column(nullable = false, length = 100, unique = true)
  private String nombre;
}
