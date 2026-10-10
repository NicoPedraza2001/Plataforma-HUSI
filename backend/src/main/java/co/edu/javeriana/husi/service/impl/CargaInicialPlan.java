package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.dto.request.CargaInicialRequest.Fila;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse.Detalle;
import java.time.LocalDate;
import java.util.*;

/**
 * Simulación sin escrituras: cada fila válida reserva sus relaciones y su cupo. Si la
 * rotación ya no tiene cupo, la fila igual se importa y queda en lista de espera.
 *
 * <p>Trabaja sobre una copia en memoria de las tablas. Los registros que habría que crear se
 * acumulan en {@link #registrosNuevos} con ids temporales negativos; al confirmar, el
 * servicio los guarda en ese mismo orden y traduce cada id temporal al id real.
 */
final class CargaInicialPlan {

  /** Fila que la importación insertaría en {@code tabla} con los {@code valores} dados. */
  record RegistroNuevo(String tabla, Map<String, Object> valores) {}

  private static final List<String> TABLAS_IMPORTABLES = List.of(
    "universidad",
    "programa",
    "residente",
    "servicio",
    "asignatura",
    "rotacion",
    "rotacion_residente"
  );

  /** Copia en memoria de cada tabla (nombre de tabla → filas). */
  private final Map<String, List<Map<String, Object>>> tablas;
  final List<RegistroNuevo> registrosNuevos = new ArrayList<>();
  final List<Detalle> detalles = new ArrayList<>();
  private final boolean crearFaltantes;
  private int siguienteIdTemporal = -1;

  CargaInicialPlan(
    Map<String, List<Map<String, Object>>> tablasActuales,
    CargaInicialRequest solicitud
  ) {
    tablas = copiar(tablasActuales);
    crearFaltantes = solicitud.crearFaltantes();
    Set<Integer> numerosDeFila = new HashSet<>();
    for (Fila fila : solicitud.filas()) {
      if (
        fila == null ||
        fila.fila() == null ||
        fila.fila() < 1 ||
        !numerosDeFila.add(fila.fila())
      ) throw new IllegalArgumentException(
        "Las filas deben tener números positivos únicos."
      );
      // Punto de restauración: si la fila se rechaza, se deshace lo que alcanzó a crear.
      Map<String, Integer> tamanoPorTabla = new HashMap<>();
      tablas.forEach((tabla, filas) -> tamanoPorTabla.put(tabla, filas.size()));
      int primerRegistroDeLaFila = registrosNuevos.size();
      try {
        String avisoEspera = procesarFila(fila);
        String tablasCreadas = registrosNuevos
          .subList(primerRegistroDeLaFila, registrosNuevos.size())
          .stream()
          .map(RegistroNuevo::tabla)
          .distinct()
          .reduce((anterior, siguiente) -> anterior + ", " + siguiente)
          .orElse("");
        String resumenCreaciones = tablasCreadas.isEmpty()
          ? ""
          : " Se crearán: " + tablasCreadas + ".";
        detalles.add(
          avisoEspera == null
            ? new Detalle(
                fila.fila(),
                "VALIDO",
                "Residente y rotación relacionados con cupo." + resumenCreaciones
              )
            : new Detalle(fila.fila(), "ESPERA", avisoEspera + resumenCreaciones)
        );
      } catch (FilaRechazada rechazo) {
        tablas.forEach((tabla, filas) ->
          filas.subList(tamanoPorTabla.get(tabla), filas.size()).clear()
        );
        registrosNuevos
          .subList(primerRegistroDeLaFila, registrosNuevos.size())
          .clear();
        detalles.add(
          new Detalle(fila.fila(), rechazo.estado, rechazo.getMessage())
        );
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
            .map(fila -> (Map<String, Object>) new LinkedHashMap<>(fila))
            .toList()
        )
      )
    );
    return copia;
  }

  static String texto(Object valor) {
    return valor == null ? "" : valor.toString().strip();
  }

  static int entero(Object valor) {
    return ((Number) valor).intValue();
  }

  /** Limpia el valor de una celda y valida que exista, su longitud y que no sea una fórmula. */
  private String leerCampo(
    String valor,
    String nombreCampo,
    int longitudMaxima,
    boolean obligatorio
  ) {
    String limpio = texto(valor);
    if (obligatorio && limpio.isEmpty()) rechazarFila("Falta " + nombreCampo + ".");
    if (limpio.length() > longitudMaxima) rechazarFila(
      nombreCampo + " admite hasta " + longitudMaxima + " caracteres."
    );
    if (limpio.contains("[FÓRMULA:")) rechazarFila(
      "Reemplaza la fórmula de " + nombreCampo + " por su valor."
    );
    return limpio;
  }

  private int leerCupo(String valor, String nombreCampo) {
    try {
      if (!texto(valor).matches("[0-9]+")) throw new Exception();
      return Integer.parseInt(valor.strip());
    } catch (Exception e) {
      throw new FilaRechazada(
        "ERROR",
        nombreCampo +
          " debe ser un entero entre 0 y 2147483647 para crear el servicio."
      );
    }
  }

  /**
   * Busca en {@code tabla} la única fila cuyos campos coinciden (sin distinguir mayúsculas)
   * con {@code criterios}. Devuelve null si no hay ninguna y rechaza la fila si hay varias o
   * si la encontrada está inactiva.
   */
  private Map<String, Object> buscarRegistro(
    String tabla,
    Map<String, Object> criterios
  ) {
    var coincidencias = tablas
      .get(tabla)
      .stream()
      .filter(registro ->
        criterios
          .entrySet()
          .stream()
          .allMatch(criterio ->
            texto(registro.get(criterio.getKey())).equalsIgnoreCase(
              texto(criterio.getValue())
            )
          )
      )
      .toList();
    if (coincidencias.size() > 1) rechazarFila(
      "Coincidencia ambigua en " + tabla + ". Corrige los nombres del catálogo."
    );
    if (coincidencias.isEmpty()) return null;
    var encontrado = coincidencias.getFirst();
    if (
      encontrado.containsKey("estado") &&
      !"Activo".equals(encontrado.get("estado"))
    ) rechazarFila(
      "El registro de " +
        tabla +
        " existe pero está inactivo. Revísalo en su módulo."
    );
    return encontrado;
  }

  /** Agrega a la simulación un registro que no existe, con un id temporal negativo. */
  private Map<String, Object> registrarNuevo(
    String tabla,
    Map<String, Object> valores
  ) {
    if (!crearFaltantes) rechazarFila(
      "No existe " + tabla + ". Regístralo o habilita crear datos faltantes."
    );
    var registro = new LinkedHashMap<>(valores);
    if (!tabla.equals("rotacion") && !tabla.equals("rotacion_residente")) {
      registro.put("id_" + tabla, siguienteIdTemporal--);
      registro.put("estado", "Activo");
    }
    tablas.get(tabla).add(registro);
    registrosNuevos.add(new RegistroNuevo(tabla, new LinkedHashMap<>(registro)));
    return registro;
  }

  /** Un valor del Excel no puede contradecir al registro existente; si viene vacío se ignora. */
  private void validarCoincidencia(
    Map<String, Object> registro,
    String columna,
    String valorExcel,
    String descripcion
  ) {
    if (
      !valorExcel.isEmpty() &&
      !texto(registro.get(columna)).equalsIgnoreCase(valorExcel)
    ) rechazarFila(
      descripcion +
        " no coincide con el registro existente. No se sobrescribirá; corrígelo antes de importar."
    );
  }

  /** Devuelve null si la fila toma cupo, o el aviso de su posición en la lista de espera. */
  private String procesarFila(Fila fila) {
    String nombreUniversidad = leerCampo(fila.universidad(), "universidad", 150, true);
    String pais = leerCampo(fila.pais(), "país", 100, false);
    String nombrePrograma = leerCampo(fila.programa(), "programa", 150, true);
    String nombreResidente = leerCampo(fila.residente(), "nombre del residente", 200, true);
    String documento = leerCampo(fila.documento(), "documento", 50, true);
    String tipoDocumento = leerCampo(fila.tipoDocumento(), "tipo de documento", 30, true);
    String perfilAcademico = leerCampo(
      fila.perfilAcademico(),
      "perfil académico",
      100,
      false
    );
    // Los catálogos del residente no se crean desde el Excel: se aceptan el
    // código (CC) o el nombre (Cédula de ciudadanía) y se guarda el código.
    var tipoEncontrado = buscarRegistro(
      "tipo_documento",
      Map.of("codigo", tipoDocumento)
    );
    if (tipoEncontrado == null) tipoEncontrado = buscarRegistro(
      "tipo_documento",
      Map.of("nombre", tipoDocumento)
    );
    if (tipoEncontrado == null) rechazarFila(
      "Tipo de documento «" + tipoDocumento + "» no reconocido. Usa un código del catálogo (CC, TI, CE, PA…)."
    );
    tipoDocumento = texto(tipoEncontrado.get("codigo"));
    if (!perfilAcademico.isEmpty()) {
      var perfilEncontrado = buscarRegistro(
        "perfil_academico",
        Map.of("nombre", perfilAcademico)
      );
      if (perfilEncontrado == null) rechazarFila(
        "Perfil académico «" + perfilAcademico + "» no reconocido. Usa Pregrado o Posgrado."
      );
      perfilAcademico = texto(perfilEncontrado.get("nombre"));
    }
    String nombreServicio = leerCampo(fila.servicio(), "servicio", 150, true);
    String nombreAsignatura = leerCampo(fila.asignatura(), "asignatura", 200, true);
    LocalDate inicio = leerFecha(fila.inicio(), "inicio");
    LocalDate fin = leerFecha(fila.fin(), "fin");
    if (inicio.getYear() < 1900 || fin.getYear() > 9999) rechazarFila(
      "Las fechas deben estar entre los años 1900 y 9999."
    );
    if (fin.isBefore(inicio)) rechazarFila(
      "La fecha de fin (" + fin + ") es anterior a la fecha de inicio (" + inicio + ")."
    );

    var universidad = buscarRegistro(
      "universidad",
      Map.of("nombre_universidad", nombreUniversidad)
    );
    if (universidad == null) {
      if (pais.isEmpty()) rechazarFila("Indica el país para crear la universidad.");
      universidad = registrarNuevo(
        "universidad",
        Map.of("nombre_universidad", nombreUniversidad, "pais", pais)
      );
    } else validarCoincidencia(
      universidad,
      "pais",
      pais,
      "El país de la universidad"
    );

    var criteriosPrograma = Map.<String, Object>of(
      "id_universidad",
      universidad.get("id_universidad"),
      "nombre_programa",
      nombrePrograma
    );
    if (!ProgramasDeConvenio.esUniversidadPropia(nombreUniversidad)) {
      ProgramasDeConvenio.buscarErrorDeConvenioSegunPais(
        nombrePrograma,
        nombreUniversidad,
        texto(universidad.get("pais"))
      ).ifPresent(this::rechazarFila);
    }
    var programa = buscarRegistro("programa", criteriosPrograma);
    if (programa == null) {
      if (
        !ProgramasDeConvenio.esUniversidadPropia(nombreUniversidad) &&
        ProgramasDeConvenio.buscarConvenio(nombrePrograma).isEmpty()
      ) rechazarFila(ProgramasDeConvenio.MENSAJE_SOLO_CONVENIOS);
      programa = registrarNuevo("programa", criteriosPrograma);
    }

    var residente = buscarRegistro(
      "residente",
      Map.of("numero_documento", documento)
    );
    if (residente == null) residente = registrarNuevo(
      "residente",
      Map.of(
        "id_programa",
        programa.get("id_programa"),
        "nombre_completo",
        nombreResidente,
        "tipo_documento",
        tipoDocumento,
        "numero_documento",
        documento,
        "perfil_academico",
        perfilAcademico
      )
    );
    else {
      validarCoincidencia(
        residente,
        "id_programa",
        texto(programa.get("id_programa")),
        "El programa del residente"
      );
      validarCoincidencia(
        residente,
        "nombre_completo",
        nombreResidente,
        "El nombre del residente"
      );
      validarCoincidencia(
        residente,
        "tipo_documento",
        tipoDocumento,
        "El tipo de documento"
      );
      validarCoincidencia(
        residente,
        "perfil_academico",
        perfilAcademico,
        "El perfil académico"
      );
    }

    var servicio = buscarRegistro("servicio", Map.of("nombre", nombreServicio));
    if (servicio == null) {
      int cupoMaximo = leerCupo(fila.cupoMaximo(), "Cupo máximo");
      int cupoSimultaneo = leerCupo(fila.cupoSimultaneo(), "Cupo simultáneo");
      if (cupoSimultaneo > cupoMaximo) rechazarFila(
        "El cupo simultáneo no puede superar el máximo instalado."
      );
      servicio = registrarNuevo(
        "servicio",
        Map.of(
          "nombre",
          nombreServicio,
          "cupo_maximo_instalado",
          cupoMaximo,
          "cupo_simultaneo",
          cupoSimultaneo
        )
      );
    } else {
      if (!texto(fila.cupoMaximo()).isEmpty()) validarCoincidencia(
        servicio,
        "cupo_maximo_instalado",
        String.valueOf(leerCupo(fila.cupoMaximo(), "Cupo máximo")),
        "El cupo máximo"
      );
      if (!texto(fila.cupoSimultaneo()).isEmpty()) validarCoincidencia(
        servicio,
        "cupo_simultaneo",
        String.valueOf(leerCupo(fila.cupoSimultaneo(), "Cupo simultáneo")),
        "El cupo simultáneo"
      );
    }

    var criteriosAsignatura = Map.<String, Object>of(
      "id_servicio",
      servicio.get("id_servicio"),
      "nombre_asignatura",
      nombreAsignatura
    );
    var asignatura = buscarRegistro("asignatura", criteriosAsignatura);
    if (asignatura == null) asignatura = registrarNuevo(
      "asignatura",
      criteriosAsignatura
    );

    var claveRotacion = Map.<String, Object>of(
      "id_asignatura",
      asignatura.get("id_asignatura"),
      "fecha_inicio",
      inicio,
      "fecha_fin",
      fin
    );
    var rotacion = tablas
      .get("rotacion")
      .stream()
      .filter(registro -> coincidenCampos(registro, claveRotacion))
      .findFirst()
      .orElse(null);
    var asignacion = new LinkedHashMap<>(claveRotacion);
    asignacion.put("id_residente", residente.get("id_residente"));
    if (
      tablas
        .get("rotacion_residente")
        .stream()
        .anyMatch(registro -> coincidenCampos(registro, asignacion))
    ) throw new FilaRechazada(
      "OMITIDO",
      "El residente ya está relacionado con esta rotación (incluidos cancelados), o la fila está repetida."
    );
    if (rotacion == null) {
      rotacion = new LinkedHashMap<>(claveRotacion);
      rotacion.put("activa", true);
      rotacion.put("cupo_disponible", servicio.get("cupo_simultaneo"));
      // Crear la rotación está permitido también cuando todos los catálogos ya existen.
      tablas.get("rotacion").add(rotacion);
      registrosNuevos.add(
        new RegistroNuevo("rotacion", new LinkedHashMap<>(rotacion))
      );
    }
    if (!Boolean.TRUE.equals(rotacion.get("activa"))) rechazarFila(
      "La rotación existe pero está inactiva."
    );
    int cuposDisponibles = entero(rotacion.get("cupo_disponible"));
    if (cuposDisponibles < 1) {
      // Al guardar, el trigger de la base la deja en «Esperando cupo» en este mismo orden.
      long posicionEnEspera =
        1 +
        tablas
          .get("rotacion_residente")
          .stream()
          .filter(registro -> coincidenCampos(registro, claveRotacion))
          .filter(registro -> "Esperando cupo".equals(texto(registro.get("estado"))))
          .count();
      asignacion.put("estado", "Esperando cupo");
      tablas.get("rotacion_residente").add(asignacion);
      registrosNuevos.add(new RegistroNuevo("rotacion_residente", asignacion));
      return (
        "Sin cupo en esta rotación: quedará en lista de espera (posición " +
        posicionEnEspera +
        ")."
      );
    }
    rotacion.put("cupo_disponible", cuposDisponibles - 1);
    asignacion.put("estado", "Asignado");
    tablas.get("rotacion_residente").add(asignacion);
    registrosNuevos.add(new RegistroNuevo("rotacion_residente", asignacion));
    return null;
  }

  /** Compara como texto exacto cada campo de {@code criterios} con el de {@code registro}. */
  private boolean coincidenCampos(
    Map<String, Object> registro,
    Map<String, Object> criterios
  ) {
    return criterios
      .entrySet()
      .stream()
      .allMatch(criterio ->
        texto(registro.get(criterio.getKey())).equals(texto(criterio.getValue()))
      );
  }

  CargaInicialResponse construirRespuesta(String huella, boolean guardado) {
    Map<String, Integer> nuevosPorTabla = new LinkedHashMap<>();
    for (String tabla : TABLAS_IMPORTABLES) nuevosPorTabla.put(
      tabla,
      (int) registrosNuevos
        .stream()
        .filter(registro -> registro.tabla().equals(tabla))
        .count()
    );
    return new CargaInicialResponse(
      huella,
      guardado,
      detalles.size(),
      contarDetalles("VALIDO"),
      contarDetalles("ESPERA"),
      contarDetalles("ERROR"),
      contarDetalles("OMITIDO"),
      nuevosPorTabla,
      detalles
        .stream()
        .map(detalle -> guardado ? detalleTrasGuardar(detalle) : detalle)
        .toList()
    );
  }

  /** Tras confirmar, los mensajes de la simulación pasan a tiempo pasado. */
  private Detalle detalleTrasGuardar(Detalle detalle) {
    return switch (detalle.estado()) {
      case "VALIDO" -> new Detalle(
        detalle.fila(),
        "CREADO",
        "Asignación creada correctamente."
      );
      case "ESPERA" -> new Detalle(
        detalle.fila(),
        "ESPERA",
        detalle.mensaje().replace("quedará en", "quedó en")
      );
      default -> detalle;
    };
  }

  private int contarDetalles(String estado) {
    return (int) detalles
      .stream()
      .filter(detalle -> detalle.estado().equals(estado))
      .count();
  }

  /**
   * Fecha AAAA-MM-DD de la fila. El mensaje nombra el valor recibido: un «31/11/2026» suele ser
   * una fecha que no existe y que Excel guardó como texto en vez de como fecha.
   */
  private LocalDate leerFecha(String valor, String nombreFecha) {
    String fecha = texto(valor).strip();
    if (fecha.isEmpty()) rechazarFila("Falta la fecha de " + nombreFecha + ".");
    try {
      return LocalDate.parse(fecha);
    } catch (java.time.format.DateTimeParseException e) {
      throw new FilaRechazada(
        "ERROR",
        "La fecha de " +
          nombreFecha +
          " «" +
          fecha +
          "» no es válida: no existe en el calendario o no tiene formato AAAA-MM-DD."
      );
    }
  }

  private void rechazarFila(String mensaje) {
    throw new FilaRechazada("ERROR", mensaje);
  }

  /** Interrumpe el procesamiento de una fila; {@code estado} es ERROR u OMITIDO. */
  private static final class FilaRechazada extends RuntimeException {

    final String estado;

    FilaRechazada(String estado, String mensaje) {
      super(mensaje);
      this.estado = estado;
    }
  }
}
