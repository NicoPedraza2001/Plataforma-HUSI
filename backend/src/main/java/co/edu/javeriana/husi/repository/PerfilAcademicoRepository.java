package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.PerfilAcademico;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilAcademicoRepository
  extends JpaRepository<PerfilAcademico, Integer> {
  Optional<PerfilAcademico> findByNombreIgnoreCase(String nombre);

  List<PerfilAcademico> findAllByOrderByNombre();
}
