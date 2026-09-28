package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.dto.request.CargaInicialRequest.Fila;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse.Detalle;
import java.time.LocalDate;
import java.util.*;

/** Simulación sin escrituras: cada fila válida reserva sus relaciones y su cupo. */
final class CargaInicialPlan {

  record Alta(String tabla, Map<String, Object> datos) {}

  private Map<String, List<Map<String, Object>>> datos;
  final List<Alta> altas = new ArrayList<>();
  final List<Detalle> detalles = new ArrayList<>();
  private final boolean crear;
  private int siguiente = -1;

  CargaInicialPlan(
    Map<String, List<Map<String, Object>>> origen,
    CargaInicialRequest solicitud
  ) {
    datos = copiar(origen);
    crear = solicitud.crearFaltantes();
    Set<Integer> filas = new HashSet<>();
    for (Fila f : solicitud.filas()) {
      if (
        f == null || f.fila() == null || f.fila() < 1 || !filas.add(f.fila())
      ) throw new IllegalArgumentException(
        "Las filas deben tener números positivos únicos."
      );
      Map<String, Integer> tamanos = new HashMap<>();
      datos.forEach((tabla, lista) -> tamanos.put(tabla, lista.size()));
      int inicio = altas.size();
      try {
        procesar(f);
        String nuevos = altas
          .subList(inicio, altas.size())
          .stream()
          .map(Alta::tabla)
          .distinct()
          .reduce((a, b) -> a + ", " + b)
          .orElse("");
        detalles.add(
          new Detalle(
            f.fila(),
            "VALIDO",
            "Residente y rotación relacionados con cupo." +
              (nuevos.isEmpty() ? "" : " Se crearán: " + nuevos + ".")
          )
        );
      } catch (Incidencia e) {
        datos.forEach((tabla, lista) ->
          lista.subList(tamanos.get(tabla), lista.size()).clear()
        );
        altas.subList(inicio, altas.size()).clear();
        detalles.add(new Detalle(f.fila(), e.estado, e.getMessage()));
      }
    }
  }

  private static Map<String, List<Map<String, Object>>> copiar(
    Map<String, List<Map<String, Object>>> origen
  ) {
    Map<String, List<Map<String, Object>>> copia = new LinkedHashMap<>();
    origen.forEach((tabla, filas) ->
      copia.put(
        tabla,
        new ArrayList<>(
          filas
            .stream()
            .map(f -> (Map<String, Object>) new LinkedHashMap<>(f))
            .toList()
        )
      )
    );
    return copia;
  }

  static String texto(Object x) {
    return x == null ? "" : x.toString().strip();
  }

  static int numero(Object x) {
    return ((Number) x).intValue();
  }

  private String campo(
    String valor,
    String nombre,
    int max,
    boolean obligatorio
  ) {
    String v = texto(valor);
    if (obligatorio && v.isEmpty()) fallo("Falta " + nombre + ".");
    if (v.length() > max) fallo(
      nombre + " admite hasta " + max + " caracteres."
    );
    if (v.contains("[FÓRMULA:")) fallo(
      "Reemplaza la fórmula de " + nombre + " por su valor."
    );
    return v;
  }

  private int cupo(String valor, String nombre) {
    try {
      if (!texto(valor).matches("[0-9]+")) throw new Exception();
      return Integer.parseInt(valor.strip());
    } catch (Exception e) {
      throw new Incidencia(
        "ERROR",
        nombre +
          " debe ser un entero entre 0 y 2147483647 para crear el servicio."
      );
    }
  }

  private Map<String, Object> buscar(String tabla, Map<String, Object> clave) {
    var encontrados = datos
      .get(tabla)
      .stream()
      .filter(r ->
        clave
          .entrySet()
          .stream()
          .allMatch(e ->
            texto(r.get(e.getKey())).equalsIgnoreCase(texto(e.getValue()))
          )
      )
      .toList();
    if (encontrados.size() > 1) fallo(
      "Coincidencia ambigua en " + tabla + ". Corrige los nombres del catálogo."
    );
    if (encontrados.isEmpty()) return null;
    var encontrado = encontrados.getFirst();
    if (
      encontrado.containsKey("estado") &&
      !"Activo".equals(encontrado.get("estado"))
    ) fallo(
      "El registro de " +
        tabla +
        " existe pero está inactivo. Revísalo en su módulo."
    );
    return encontrado;
  }

  private Map<String, Object> alta(String tabla, Map<String, Object> valores) {
    if (!crear) fallo(
      "No existe " + tabla + ". Regístralo o habilita crear datos faltantes."
    );
    var fila = new LinkedHashMap<>(valores);
    if (!tabla.equals("rotacion") && !tabla.equals("rotacion_residente")) {
      fila.put("id_" + tabla, siguiente--);
      fila.put("estado", "Activo");
    }
    datos.get(tabla).add(fila);
    altas.add(new Alta(tabla, new LinkedHashMap<>(fila)));
    return fila;
  }

  private void coincide(
    Map<String, Object> registro,
    String campo,
    String valor,
    String etiqueta
  ) {
    if (
      !valor.isEmpty() && !texto(registro.get(campo)).equalsIgnoreCase(valor)
    ) fallo(
      etiqueta +
        " no coincide con el registro existente. No se sobrescribirá; corrígelo antes de importar."
    );
  }

  private void procesar(Fila f) {
    String universidad = campo(f.universidad(), "universidad", 150, true),
      pais = campo(f.pais(), "país", 100, false);
    String programa = campo(f.programa(), "programa", 150, true),
      nombre = campo(f.residente(), "nombre del residente", 200, true);
    String documento = campo(f.documento(), "documento", 50, true),
      tipo = campo(f.tipoDocumento(), "tipo de documento", 30, true);
    String perfil = campo(f.perfilAcademico(), "perfil académico", 100, false);
    String servicio = campo(f.servicio(), "servicio", 150, true),
      asignatura = campo(f.asignatura(), "asignatura", 200, true);
    LocalDate inicio, fin;
    try {
      inicio = LocalDate.parse(texto(f.inicio()));
      fin = LocalDate.parse(texto(f.fin()));
    } catch (Exception e) {
      throw new Incidencia(
        "ERROR",
        "Fechas inválidas. Usa celdas de fecha o AAAA-MM-DD."
      );
    }
    if (
      inicio.getYear() < 1900 || fin.getYear() > 9999 || fin.isBefore(inicio)
    ) fallo("Revisa el orden de las fechas y el rango 1900–9999.");

    var u = buscar("universidad", Map.of("nombre_universidad", universidad));
    if (u == null) {
      if (pais.isEmpty()) fallo("Indica el país para crear la universidad.");
      u = alta(
        "universidad",
        Map.of("nombre_universidad", universidad, "pais", pais)
      );
    } else coincide(u, "pais", pais, "El país de la universidad");
    var p = buscar(
      "programa",
      Map.of(
        "id_universidad",
        u.get("id_universidad"),
        "nombre_programa",
        programa
      )
    );
    if (p == null) p = alta(
      "programa",
      Map.of(
        "id_universidad",
        u.get("id_universidad"),
        "nombre_programa",
        programa
      )
    );
    var r = buscar("residente", Map.of("numero_documento", documento));
    if (r == null) r = alta(
      "residente",
      Map.of(
        "id_programa",
        p.get("id_programa"),
        "nombre_completo",
        nombre,
        "tipo_documento",
        tipo,
        "numero_documento",
        documento,
        "perfil_academico",
        perfil
      )
    );
    else {
      coincide(
        r,
        "id_programa",
        texto(p.get("id_programa")),
        "El programa del residente"
      );
      coincide(r, "nombre_completo", nombre, "El nombre del residente");
      coincide(r, "tipo_documento", tipo, "El tipo de documento");
      coincide(r, "perfil_academico", perfil, "El perfil académico");
    }
    var s = buscar("servicio", Map.of("nombre", servicio));
    if (s == null) {
      int max = cupo(f.cupoMaximo(), "Cupo máximo"),
        simultaneo = cupo(f.cupoSimultaneo(), "Cupo simultáneo");
      if (simultaneo > max) fallo(
        "El cupo simultáneo no puede superar el máximo instalado."
      );
      s = alta(
        "servicio",
        Map.of(
          "nombre",
          servicio,
          "cupo_maximo_instalado",
          max,
          "cupo_simultaneo",
          simultaneo
        )
      );
    } else {
      if (!texto(f.cupoMaximo()).isEmpty()) coincide(
        s,
        "cupo_maximo_instalado",
        String.valueOf(cupo(f.cupoMaximo(), "Cupo máximo")),
        "El cupo máximo"
      );
      if (!texto(f.cupoSimultaneo()).isEmpty()) coincide(
        s,
        "cupo_simultaneo",
        String.valueOf(cupo(f.cupoSimultaneo(), "Cupo simultáneo")),
        "El cupo simultáneo"
      );
    }
    var a = buscar(
      "asignatura",
      Map.of(
        "id_servicio",
        s.get("id_servicio"),
        "nombre_asignatura",
        asignatura
      )
    );
    if (a == null) a = alta(
      "asignatura",
      Map.of(
        "id_servicio",
        s.get("id_servicio"),
        "nombre_asignatura",
        asignatura
      )
    );
    var clave = Map.<String, Object>of(
      "id_asignatura",
      a.get("id_asignatura"),
      "fecha_inicio",
      inicio,
      "fecha_fin",
      fin
    );
    var rot = datos
      .get("rotacion")
      .stream()
      .filter(x -> mismaClave(x, clave))
      .findFirst()
      .orElse(null);
    var relacion = new LinkedHashMap<>(clave);
    relacion.put("id_residente", r.get("id_residente"));
    if (
      datos
        .get("rotacion_residente")
        .stream()
        .anyMatch(x -> mismaClave(x, relacion))
    ) throw new Incidencia(
      "OMITIDO",
      "El residente ya está relacionado con esta rotación (incluidos cancelados), o la fila está repetida."
    );
    if (rot == null) {
      rot = new LinkedHashMap<>(clave);
      rot.put("activa", true);
      rot.put("cupo_disponible", s.get("cupo_simultaneo"));
      // Crear la rotación está permitido también cuando todos los catálogos ya existen.
      datos.get("rotacion").add(rot);
      altas.add(new Alta("rotacion", new LinkedHashMap<>(rot)));
    }
    if (!Boolean.TRUE.equals(rot.get("activa"))) fallo(
      "La rotación existe pero está inactiva."
    );
    int disponibles = numero(rot.get("cupo_disponible"));
    if (disponibles < 1) throw new Incidencia(
      "ADVERTENCIA",
      "Sin cupo en esta rotación. No se importará esta fila. Corrige el período o la asignatura, o revisa su prioridad."
    );
    rot.put("cupo_disponible", disponibles - 1);
    datos.get("rotacion_residente").add(relacion);
    altas.add(new Alta("rotacion_residente", relacion));
  }

  private boolean mismaClave(
    Map<String, Object> fila,
    Map<String, Object> clave
  ) {
    return clave
      .entrySet()
      .stream()
      .allMatch(e -> texto(fila.get(e.getKey())).equals(texto(e.getValue())));
  }

  CargaInicialResponse respuesta(String huella, boolean guardado) {
    Map<String, Integer> nuevos = new LinkedHashMap<>();
    for (String tabla : List.of(
      "universidad",
      "programa",
      "residente",
      "servicio",
      "asignatura",
      "rotacion",
      "rotacion_residente"
    ))
      nuevos.put(
        tabla,
        (int) altas
          .stream()
          .filter(a -> a.tabla().equals(tabla))
          .count()
      );
    return new CargaInicialResponse(
      huella,
      guardado,
      detalles.size(),
      contar("VALIDO"),
      contar("ADVERTENCIA"),
      contar("ERROR"),
      contar("OMITIDO"),
      nuevos,
      detalles
        .stream()
        .map(d ->
          guardado && d.estado().equals("VALIDO")
            ? new Detalle(
                d.fila(),
                "CREADO",
                "Asignación creada correctamente."
              )
            : d
        )
        .toList()
    );
  }

  private int contar(String estado) {
    return (int) detalles
      .stream()
      .filter(d -> d.estado().equals(estado))
      .count();
  }

  private void fallo(String mensaje) {
    throw new Incidencia("ERROR", mensaje);
  }

  private static final class Incidencia extends RuntimeException {

    final String estado;

    Incidencia(String estado, String mensaje) {
      super(mensaje);
      this.estado = estado;
    }
  }
}
