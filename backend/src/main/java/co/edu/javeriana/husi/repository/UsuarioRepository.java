package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Usuario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
  @Query(
    value = "select exists(select 1 from usuario u join rol r using(id_rol) where u.id_usuario=:id and lower(r.nombre_rol)='coordinador')",
    nativeQuery = true
  )
  boolean tieneRolCoordinador(Integer id);

  @Query(
    value = "select u.* from usuario u join rol r using(id_rol) where u.estado='Activo' and lower(r.nombre_rol)='coordinador' and not exists(select 1 from coordinador c where c.id_usuario=u.id_usuario) order by lower(u.nombre),lower(u.apellido),u.id_usuario",
    nativeQuery = true
  )
  java.util.List<Usuario> usuariosDisponiblesParaCoordinacion();

  @Query(
    value = "select exists(select 1 from coordinador where id_usuario=:id)",
    nativeQuery = true
  )
  boolean tieneCoordinacion(Integer id);

  boolean existsByCorreoIgnoreCaseAndIdNot(String valor, Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Usuario e where e.id=:id")
  Optional<Usuario> bloquearPorId(Integer id);

  @Query(
    value = "select (select count(*) from coordinador where id_usuario=:id)+(select count(*) from historial_auditoria where id_usuario=:id)",
    nativeQuery = true
  )
  long contarReferencias(Integer id);
}
