package co.edu.javeriana.husi.mapper;
import org.springframework.stereotype.Component;
import co.edu.javeriana.husi.model.entity.Universidad;
import co.edu.javeriana.husi.dto.response.UniversidadResponse;
@Component
public class UniversidadMapper {
 public UniversidadResponse response(Universidad u) { return new UniversidadResponse(u.getId(),u.getNombre(),u.getPais(),u.getPerfil(),u.getEstado()); }
}
