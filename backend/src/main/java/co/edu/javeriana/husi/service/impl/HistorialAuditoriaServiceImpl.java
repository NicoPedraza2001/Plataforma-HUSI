package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.HistorialDetalleResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.mapper.HistorialAuditoriaMapper;
import co.edu.javeriana.husi.model.entity.HistorialAuditoria;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.HistorialAuditoriaRepository;
import co.edu.javeriana.husi.security.UsuarioEnSesion;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import co.edu.javeriana.husi.util.ImportacionEnCurso;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HistorialAuditoriaServiceImpl implements HistorialAuditoriaService {

  public static final String CLAVE_ARCHIVO = "Archivo";
  public static final String CLAVE_ESTADO_IMPORTACION = "Estado de la importación";
  private static final String SEPARADOR = " · ";
  private static final int LONGITUD_MAXIMA_DETALLE = 500;
  private static final int LONGITUD_MAXIMA_REGISTRO = 250;
  /** Campos de una creación que se resumen en la columna «Detalle»; el resto queda en el detalle. */
  private static final int CAMPOS_EN_RESUMEN_DE_CREACION = 3;

  private final HistorialAuditoriaRepository historialRepository;
  private final HistorialAuditoriaMapper historialMapper;
  private final UsuarioEnSesion usuarioEnSesion;

  @Transactional
  public void registrar(
    AccionAuditoria accion,
    ModuloAuditoria modulo,
    FotografiaRegistro antes,
    FotografiaRegistro despues
  ) {
    Optional<ImportacionEnCurso.Lote> importacion = ImportacionEnCurso.consultar();
    if (importacion.isPresent()) {
      sumarAlResumenDeImportacion(importacion.get(), accion, modulo);
      return;
    }
    Map<String, Object> datosAnteriores = antes == null ? null : antes.datos();
    Map<String, Object> datosNuevos = despues == null ? null : despues.datos();
    // Guardar un formulario sin modificar nada no es un cambio.
    if (datosAnteriores != null && datosAnteriores.equals(datosNuevos)) return;

    HistorialAuditoria historial = crearFila(accion, modulo);
    historial.setRegistro(
      recortar((despues != null ? despues : antes).nombre(), LONGITUD_MAXIMA_REGISTRO)
    );
    historial.setDetalle(describirCambio(datosAnteriores, datosNuevos));
    historial.setDatosAnteriores(historialMapper.aJson(datosAnteriores));
    historial.setDatosNuevos(historialMapper.aJson(datosNuevos));
    historialRepository.save(historial);
  }

  public <T> T ejecutarComoImportacion(String descripcion, Supplier<T> importacion) {
    if (ImportacionEnCurso.consultar().isPresent()) return importacion.get();
    ImportacionEnCurso.iniciar(
      new ImportacionEnCurso.Lote(UUID.randomUUID().toString(), descripcion, null)
    );
    try {
      return importacion.get();
    } finally {
      ImportacionEnCurso.terminar();
    }
  }

  @Transactional
  public void agregarResultadoImportacion(Map<String, Object> resultado) {
    ImportacionEnCurso.Lote lote = ImportacionEnCurso.consultar().orElseThrow(() ->
      new IllegalStateException("No hay una importación en curso.")
    );
    HistorialAuditoria resumen = buscarOCrearResumen(lote);
    Map<String, Object> datos = historialMapper.leerJson(resumen.getDatosNuevos());
    // Se quitan antes de agregarlos para que el resultado final quede al final del resumen.
    resultado.keySet().forEach(datos::remove);
    datos.putAll(resultado);
    guardarResumen(resumen, datos);
  }

  @Transactional(readOnly = true)
  public HistorialDetalleResponse consultar(Integer id) {
    return historialRepository
      .findWithUsuarioById(id)
      .map(historialMapper::toResponse)
      .orElseThrow(() ->
        new NegocioException(HttpStatus.NOT_FOUND, "No se encontró el movimiento.")
      );
  }

  // ---------- Importaciones: una fila por lote ----------

  /** Cuenta el cambio en la fila resumen del lote: «Residentes creados: 120». */
  private void sumarAlResumenDeImportacion(
    ImportacionEnCurso.Lote lote,
    AccionAuditoria accion,
    ModuloAuditoria modulo
  ) {
    HistorialAuditoria resumen = buscarOCrearResumen(lote);
    Map<String, Object> datos = historialMapper.leerJson(resumen.getDatosNuevos());
    String clave = modulo.etiqueta() + " " + accion.participioPlural(modulo.esFemenino());
    datos.merge(clave, 1, (cantidadActual, uno) -> ((Number) cantidadActual).intValue() + 1);
    guardarResumen(resumen, datos);
  }

  private HistorialAuditoria buscarOCrearResumen(ImportacionEnCurso.Lote lote) {
    return historialRepository
      .bloquearPorLote(lote.id())
      .orElseGet(() -> {
        HistorialAuditoria resumen = crearFila(
          AccionAuditoria.IMPORTO,
          ModuloAuditoria.IMPORTACIONES
        );
        resumen.setLoteImportacion(lote.id());
        resumen.setRegistro(recortar(lote.descripcion(), LONGITUD_MAXIMA_REGISTRO));
        Map<String, Object> datosIniciales = new LinkedHashMap<>();
        if (lote.archivo() != null) datosIniciales.put(CLAVE_ARCHIVO, lote.archivo());
        datosIniciales.put(CLAVE_ESTADO_IMPORTACION, "En curso");
        resumen.setDatosNuevos(historialMapper.aJson(datosIniciales));
        return resumen;
      });
  }

  private void guardarResumen(HistorialAuditoria resumen, Map<String, Object> datos) {
    resumen.setDatosNuevos(historialMapper.aJson(datos));
    resumen.setDetalle(
      recortar(
        unirCampos(
          datos
            .entrySet()
            .stream()
            .filter(campo -> !CLAVE_ARCHIVO.equals(campo.getKey()))
            .toList()
        ),
        LONGITUD_MAXIMA_DETALLE
      )
    );
    historialRepository.saveAndFlush(resumen);
  }

  // ---------- Auxiliares ----------

  private HistorialAuditoria crearFila(AccionAuditoria accion, ModuloAuditoria modulo) {
    HistorialAuditoria historial = new HistorialAuditoria();
    historial.setUsuario(usuarioEnSesion.consultar());
    historial.setAccion(accion.etiqueta());
    historial.setModulo(modulo.etiqueta());
    historial.setFecha(OffsetDateTime.now());
    return historial;
  }

  /**
   * Resumen de una línea para la columna «Detalle»: los primeros campos al crear,
   * «campo: anterior → nuevo» al modificar y una nota al eliminar.
   */
  private String describirCambio(
    Map<String, Object> datosAnteriores,
    Map<String, Object> datosNuevos
  ) {
    String descripcion;
    if (datosAnteriores == null) descripcion = unirCampos(
      datosNuevos.entrySet().stream().limit(CAMPOS_EN_RESUMEN_DE_CREACION).toList()
    );
    else if (datosNuevos == null) descripcion = "Eliminado definitivamente.";
    else {
      List<String> camposModificados = new ArrayList<>();
      datosNuevos.forEach((campo, valorNuevo) -> {
        Object valorAnterior = datosAnteriores.get(campo);
        if (!Objects.equals(valorAnterior, valorNuevo)) camposModificados.add(
          campo + ": " + mostrarValor(valorAnterior) + " → " + mostrarValor(valorNuevo)
        );
      });
      descripcion = String.join(SEPARADOR, camposModificados);
    }
    return recortar(descripcion, LONGITUD_MAXIMA_DETALLE);
  }

  private String unirCampos(List<Map.Entry<String, Object>> campos) {
    return String.join(
      SEPARADOR,
      campos
        .stream()
        .map(campo -> campo.getKey() + ": " + mostrarValor(campo.getValue()))
        .toList()
    );
  }

  private String mostrarValor(Object valor) {
    return valor == null ? "—" : valor.toString();
  }

  private String recortar(String texto, int longitudMaxima) {
    if (texto == null || texto.length() <= longitudMaxima) return texto;
    return texto.substring(0, longitudMaxima - 1) + "…";
  }
}
