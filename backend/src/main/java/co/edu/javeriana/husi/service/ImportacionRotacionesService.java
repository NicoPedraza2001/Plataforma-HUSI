package co.edu.javeriana.husi.service;
import co.edu.javeriana.husi.dto.request.ImportacionRotacionesRequest;
import co.edu.javeriana.husi.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class ImportacionRotacionesService {
 private final RotacionService rotaciones;
 // Cada creación usa su propia transacción; validar nunca escribe.
 public Map<String,Object> procesar(ImportacionRotacionesRequest request,boolean guardar) {
  if(request==null||request.filas()==null||request.filas().isEmpty()||request.filas().size()>1000) throw new NegocioException(HttpStatus.BAD_REQUEST,"Selecciona entre 1 y 1000 filas.");
  Set<Integer> numeros=new HashSet<>();
  for(var f:request.filas()) if(f==null||f.fila()==null||f.fila()<1||!numeros.add(f.fila())) throw new NegocioException(HttpStatus.BAD_REQUEST,"Los números de fila deben ser positivos y distintos.");
  List<Map<String,Object>> filas=new ArrayList<>(); Set<String> vistos=new HashSet<>();
  for(var f:request.filas()) {
   String estado="VALIDO",mensaje="Se creará una rotación activa.";
   try {
    var datos=rotaciones.validarFila(f);
    if(!vistos.add(datos.toString())) { estado="OMITIDO"; mensaje="Rotación repetida dentro del archivo."; }
    else if(guardar) { rotaciones.crear(datos); estado="CREADO"; mensaje="Rotación guardada."; }
   } catch(NegocioException e) { estado=e.getStatus()==HttpStatus.CONFLICT?"OMITIDO":"ERROR"; mensaje=e.getMessage(); }
   catch(DataAccessException e) { estado="ERROR"; mensaje="No se pudo guardar o consultar la fila. Vuelve a validar; las rotaciones existentes se omitirán."; }
   filas.add(Map.of("fila",f.fila(),"estado",estado,"mensaje",mensaje));
  }
  Map<String,Object> resultado=new LinkedHashMap<>(); resultado.put("filas",filas); resultado.put("total",filas.size());
  for(String e:List.of("VALIDO","CREADO","OMITIDO","ERROR")) resultado.put(e,filas.stream().filter(f->e.equals(f.get("estado"))).count());
  return resultado;
 }
}
