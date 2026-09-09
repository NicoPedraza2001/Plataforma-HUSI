package co.edu.javeriana.husi.service.impl;
import co.edu.javeriana.husi.service.*;
import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class ImportacionProgramasServiceImpl implements ImportacionProgramasService {
 private final ProgramaService programas;
 // Deliberadamente sin transacción de lote: cada crear confirma o revierte su propia fila.
 public ImportacionProgramasResponse procesar(ImportacionProgramasRequest request,boolean guardar) {
  Set<String> vistos=new HashSet<>();
  Set<Integer> numeros=new HashSet<>();
  for(var fila:request.filas()) {
   if(!numeros.add(fila.fila())) throw new NegocioException(HttpStatus.BAD_REQUEST,"Cada registro debe indicar una fila de Excel diferente.");
  }
  List<ImportacionProgramasResponse.Fila> resultados=new ArrayList<>();
  for(var fila:request.filas()) {
   String estado="VALIDO",mensaje="Información completa. Se creará un programa."; Integer id=null;
   try {
    ProgramaRequest datos=programas.validarImportacion(fila.nombre(),fila.universidad());
    String clave=datos.universidadId()+"|"+datos.nombre().toLowerCase(Locale.ROOT);
    if(!vistos.add(clave)) { estado="OMITIDO"; mensaje="Programa repetido dentro del archivo."; }
    else if(guardar) { id=programas.crear(datos).id(); estado="CREADO"; mensaje="Programa guardado correctamente."; }
   } catch(NegocioException e) {
    estado=e.getStatus()==HttpStatus.CONFLICT ? "OMITIDO" : "ERROR"; mensaje=e.getMessage();
   } catch(DataAccessException e) {
    estado="ERROR"; mensaje="No se pudo guardar o consultar esta fila. Actualiza y vuelve a validar el archivo.";
   }
   resultados.add(new ImportacionProgramasResponse.Fila(fila.fila(),fila.nombre(),fila.universidad(),estado,mensaje,id));
  }
  return ImportacionProgramasResponse.de(resultados);
 }
}
