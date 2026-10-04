package co.edu.javeriana.husi.service.impl;

import static co.edu.javeriana.husi.service.impl.CargaInicialPlan.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.repository.CargaInicialRepository;
import co.edu.javeriana.husi.service.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CargaInicialServiceImpl implements CargaInicialService {

  private final CargaInicialRepository consultas;
  private final UniversidadService universidades;
  private final ProgramaService programas;
  private final ResidenteService residentes;
  private final ServicioService servicios;
  private final AsignaturaService asignaturas;
  private final RotacionService rotaciones;
  private final RotacionResidenteService relaciones;

  @Transactional(readOnly = true)
  public CargaInicialResponse validar(CargaInicialRequest d) {
    return ejecutar(d, false);
  }

  @Transactional
  public CargaInicialResponse confirmar(CargaInicialRequest d) {
    return ejecutar(d, true);
  }

  private CargaInicialResponse ejecutar(
    CargaInicialRequest d,
    boolean guardar
  ) {
    if (
      d == null ||
      d.filas() == null ||
      d.filas().isEmpty() ||
      d.filas().size() > 5000
    ) throw error("Selecciona entre 1 y 5000 filas.");
    var datos = consultas.obtener(guardar);
    String huella = huella(datos, d);
    if (guardar && !huella.equals(d.validacion())) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Los datos o los cupos cambiaron desde la revisión. Vuelve a validar antes de confirmar; no se guardó nada."
    );
    CargaInicialPlan plan;
    try {
      plan = new CargaInicialPlan(datos, d);
    } catch (IllegalArgumentException e) {
      throw error(e.getMessage());
    }
    if (guardar) {
      if (plan.altas.isEmpty()) throw error(
        "No hay filas válidas para importar."
      );
      Map<Integer, Integer> ids = new HashMap<>();
      for (var alta : plan.altas) guardar(alta, ids);
    }
    return plan.respuesta(huella, guardar);
  }

  private int id(
    Map<String, Object> fila,
    String campo,
    Map<Integer, Integer> ids
  ) {
    int valor = numero(fila.get(campo));
    return ids.getOrDefault(valor, valor);
  }

  private void guardar(Alta alta, Map<Integer, Integer> ids) {
    var f = alta.datos();
    Integer creado = null;
    switch (alta.tabla()) {
      case "universidad" -> creado = universidades
        .crear(
          new UniversidadRequest(
            texto(f.get("nombre_universidad")),
            texto(f.get("pais")),
            null
          )
        )
        .id();
      case "programa" -> creado = programas
        .crear(
          new ProgramaRequest(
            texto(f.get("nombre_programa")),
            id(f, "id_universidad", ids)
          )
        )
        .id();
      case "residente" -> creado = residentes
        .crear(
          new ResidenteRequest(
            texto(f.get("nombre_completo")),
            texto(f.get("tipo_documento")),
            texto(f.get("numero_documento")),
            texto(f.get("perfil_academico")),
            id(f, "id_programa", ids)
          )
        )
        .id();
      case "servicio" -> creado = servicios
        .crear(
          new ServicioRequest(
            texto(f.get("nombre")),
            null,
            numero(f.get("cupo_simultaneo")),
            numero(f.get("cupo_maximo_instalado"))
          )
        )
        .id();
      case "asignatura" -> creado = asignaturas
        .crear(
          new AsignaturaRequest(
            texto(f.get("nombre_asignatura")),
            id(f, "id_servicio", ids)
          )
        )
        .id();
      case "rotacion" -> rotaciones.crearHistorica(
        new RotacionRequest(
          id(f, "id_asignatura", ids),
          fecha(f, "fecha_inicio"),
          fecha(f, "fecha_fin")
        )
      );
      case "rotacion_residente" -> relaciones.asignarHistorica(
        new RotacionId(
          id(f, "id_asignatura", ids),
          fecha(f, "fecha_inicio"),
          fecha(f, "fecha_fin")
        ),
        id(f, "id_residente", ids)
      );
      default -> throw new IllegalStateException("Tabla no admitida");
    }
    if (creado != null) ids.put(numero(f.get("id_" + alta.tabla())), creado);
  }

  private LocalDate fecha(Map<String, Object> f, String campo) {
    return LocalDate.parse(texto(f.get(campo)));
  }

  private String huella(
    Map<String, List<Map<String, Object>>> datos,
    CargaInicialRequest d
  ) {
    try {
      // TreeMap hace estable el orden de columnas en PostgreSQL y H2.
      StringBuilder base = new StringBuilder();
      datos.forEach((tabla, filas) -> {
        base.append(tabla);
        filas.forEach(f -> base.append(new TreeMap<>(f)));
      });
      base.append(d.crearFaltantes()).append(d.filas());
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          base.toString().getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private NegocioException error(String mensaje) {
    return new NegocioException(HttpStatus.BAD_REQUEST, mensaje);
  }
}
