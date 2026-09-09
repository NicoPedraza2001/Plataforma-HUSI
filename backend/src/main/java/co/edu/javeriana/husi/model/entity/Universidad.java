package co.edu.javeriana.husi.model.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="universidad") @Getter @Setter
public class Universidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_universidad") private Integer id;
 @Column(name="nombre_universidad", nullable=false, length=150) private String nombre;
 @Column(nullable=false, length=20) private String estado;
}
