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

  @Column(name = "id_programa")
  private Integer programaId;

  @Column(name = "nombre_completo")
  private String nombre;

  @Column(name = "tipo_documento")
  private String tipoDocumento;

  @Column(name = "numero_documento")
  private String numeroDocumento;

  @Column(name = "perfil_academico")
  private String perfilAcademico;

  @Column
  private String estado = "Activo";
}
