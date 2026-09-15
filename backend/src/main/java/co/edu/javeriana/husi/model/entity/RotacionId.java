package co.edu.javeriana.husi.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;
@Embeddable @Data @NoArgsConstructor @AllArgsConstructor
public class RotacionId implements Serializable {
 @Column(name="id_asignatura") private Integer asignaturaId;
 @Column(name="fecha_inicio") private LocalDate inicio;
 @Column(name="fecha_fin") private LocalDate fin;
}
