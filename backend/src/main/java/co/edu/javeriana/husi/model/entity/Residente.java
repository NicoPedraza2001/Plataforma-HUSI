package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "residente")
@Getter
@Setter
public class Residente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_residente")
  private Integer id;

  @Column(name = "id_programa", nullable = false)
  private Integer programaId;

  @Column(name = "nombre_completo", nullable = false, length = 200)
  private String nombre;

  @Column(name = "tipo_documento", nullable = false, length = 30)
  private String tipoDocumento;

  @Column(
    name = "numero_documento",
    nullable = false,
    length = 50,
    unique = true
  )
  private String numeroDocumento;

  @Column(name = "perfil_academico", length = 100)
  private String perfilAcademico;

  @Column(nullable = false, length = 20)
  private String estado = "Activo";
}
