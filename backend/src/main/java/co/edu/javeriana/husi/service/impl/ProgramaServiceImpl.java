package co.edu.javeriana.husi.service.impl;
import co.edu.javeriana.husi.service.ProgramaService;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.mapper.ProgramaMapper;
import co.edu.javeriana.husi.dto.request.ProgramaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ProgramaServiceImpl implements ProgramaService {
 private final ProgramaRepository programas;
 private final UniversidadRepository universidades;
 private final ProgramaMapper mapper;
 public List<ProgramaResponse> listar() { return programas.findByEstadoOrderByNombreAscIdAsc("Activo").stream().map(mapper::response).toList(); }
 public ProgramaResponse consultar(Integer id) { return mapper.response(obtener(id)); }
 private Programa obtener(Integer id) { return programas.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"No se encontró el programa.")); }
 private Programa obtenerBloqueado(Integer id) { return programas.bloquearPorId(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"No se encontró el programa.")); }
 private String nombreValido(String nombre) {
  if(nombre==null || nombre.isBlank()) throw error(HttpStatus.BAD_REQUEST,"Falta el nombre del programa.");
  String limpio=nombre.strip();
  if(limpio.length()>150) throw error(HttpStatus.BAD_REQUEST,"El nombre admite hasta 150 caracteres.");
  return limpio;
 }
 private Universidad universidadActiva(Integer id) {
  if(id==null) throw error(HttpStatus.BAD_REQUEST,"Selecciona una universidad.");
  Universidad u=universidades.bloquearPorId(id).orElseThrow(() -> error(HttpStatus.BAD_REQUEST,"La universidad no existe."));
  if(!"Activo".equals(u.getEstado())) throw error(HttpStatus.BAD_REQUEST,"La universidad está inactiva.");
  return u;
 }
 private void sinDuplicado(Integer universidadId,String nombre,Integer excluir) {
  programas.findFirstByUniversidadIdAndNombreIgnoreCase(universidadId,nombre).ifPresent(p -> {
   if(!Objects.equals(p.getId(),excluir)) throw error(HttpStatus.CONFLICT,"Ya existe este programa en la universidad (estado: "+p.getEstado()+"). No se creará otro.");
  });
 }
 @Transactional public ProgramaResponse crear(ProgramaRequest request) {
  String nombre=nombreValido(request.nombre());
  Universidad u=universidadActiva(request.universidadId());
  sinDuplicado(u.getId(),nombre,null);
  Programa p=new Programa(); p.setNombre(nombre); p.setUniversidad(u);
  return mapper.response(programas.saveAndFlush(p));
 }
 @Transactional public ProgramaResponse actualizar(Integer id,ProgramaRequest request) {
  Programa p=obtenerBloqueado(id);
  if("Eliminado".equals(p.getEstado())) throw error(HttpStatus.CONFLICT,"El programa no está disponible.");
  String nombre=nombreValido(request.nombre());
  Universidad u=universidadActiva(request.universidadId());
  sinDuplicado(u.getId(),nombre,id);
  p.setNombre(nombre); p.setUniversidad(u);
  return mapper.response(programas.saveAndFlush(p));
 }
 @Transactional public void desactivar(Integer id) {
  Programa p=obtenerBloqueado(id); p.setEstado("Inactivo"); programas.saveAndFlush(p);
 }
 @Transactional public ProgramaResponse cambiarEstado(Integer id,String estado) {
  if(!Set.of("Activo","Inactivo").contains(estado==null?"":estado)) throw error(HttpStatus.BAD_REQUEST,"El estado debe ser Activo o Inactivo.");
  Programa p=obtenerBloqueado(id);
  if("Eliminado".equals(p.getEstado())) throw error(HttpStatus.CONFLICT,"El programa no está disponible.");
  if("Activo".equals(estado)) universidadActiva(p.getUniversidad().getId());
  p.setEstado(estado);return mapper.response(programas.saveAndFlush(p));
 }
 @Transactional public void eliminarDefinitivamente(Integer id) {
  Programa p=obtenerBloqueado(id);
  long cantidad=programas.contarResidentes(id);
  if(cantidad>0) throw error(HttpStatus.CONFLICT,"No se puede eliminar definitivamente: este programa tiene "+cantidad+" residentes asociados, incluidos los inactivos. Puedes desactivarlo para conservar su historial.");
  programas.delete(p);programas.flush();
 }
 public ProgramaRequest validarImportacion(String nombre,String universidad) {
  String limpio=nombreValido(nombre);
  if(universidad==null || universidad.isBlank()) throw error(HttpStatus.BAD_REQUEST,"Falta la universidad.");
  if(universidad.strip().length()>150) throw error(HttpStatus.BAD_REQUEST,"La universidad admite hasta 150 caracteres.");
  List<Universidad> opciones=universidades.findByNombreIgnoreCaseAndEstado(universidad.strip(),"Activo");
  if(opciones.isEmpty()) throw error(HttpStatus.BAD_REQUEST,"Universidad no encontrada o inactiva. Usa el nombre del catálogo.");
  if(opciones.size()!=1) throw error(HttpStatus.BAD_REQUEST,"El nombre de universidad es ambiguo. Revisa el catálogo.");
  Integer id=opciones.getFirst().getId();
  sinDuplicado(id,limpio,null);
  return new ProgramaRequest(limpio,id);
 }
 private static NegocioException error(HttpStatus status,String mensaje) { return new NegocioException(status,mensaje); }
}
