package co.edu.javeriana.husi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_usuario")
  private Integer id;

  @Column(name = "id_rol", nullable = false)
  private Integer rolId;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Column(name = "apellido", nullable = false, length = 100)
  private String apellido;

  @Column(name = "correo", nullable = false, length = 150, unique = true)
  private String correo;

  @Column(name = "contrasena", nullable = false, length = 255)
  private String contrasena;

  @Column(nullable = false, length = 20)
  private String estado = "Activo";
}
