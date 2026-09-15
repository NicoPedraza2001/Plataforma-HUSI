package co.edu.javeriana.husi.service;
import co.edu.javeriana.husi.dto.request.ProgramaRequest;
import co.edu.javeriana.husi.dto.response.*;
import java.util.List;
public interface ProgramaService {
 List<ProgramaResponse> listar();
 ProgramaResponse consultar(Integer id);
 ProgramaResponse crear(ProgramaRequest request);
 ProgramaResponse actualizar(Integer id,ProgramaRequest request);
 void desactivar(Integer id);
 ProgramaResponse cambiarEstado(Integer id,String estado);
 void eliminarDefinitivamente(Integer id);
 ProgramaRequest validarImportacion(String nombre,String universidad);
}
