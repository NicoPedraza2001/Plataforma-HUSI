package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "perfil_academico")
@Getter
@Setter
public class PerfilAcademico {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_perfil_academico")
  private Integer id;

  @Column(nullable = false, length = 50, unique = true)
  private String nombre;
}
