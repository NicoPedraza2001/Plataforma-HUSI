package co.edu.javeriana.husi.mapper;
import co.edu.javeriana.husi.model.entity.Programa;
import co.edu.javeriana.husi.dto.response.ProgramaResponse;
import org.springframework.stereotype.Component;
@Component
public class ProgramaMapper {
 public ProgramaResponse response(Programa p) {
  return new ProgramaResponse(p.getId(),p.getNombre(),p.getUniversidad().getId(),p.getUniversidad().getNombre(),p.getEstado());
 }
}
