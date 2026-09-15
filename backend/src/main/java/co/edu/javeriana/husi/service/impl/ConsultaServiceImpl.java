package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.repository.ConsultaRepository;
import co.edu.javeriana.husi.service.ConsultaService;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(
  readOnly = true,
  isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
)
public class ConsultaServiceImpl implements ConsultaService {

  private final ConsultaRepository repository;

  public PaginaResponse<Map<String, Object>> listar(
    String dominio,
    Map<String, String> parametros
  ) {
    validar(dominio, parametros);
    int pagina = numero(parametros.getOrDefault("pagina", "1"), 1, 1000000);
    int tamano = numero(parametros.getOrDefault("tamano", "10"), 1, 100);
    Map<String, String> filtros = new LinkedHashMap<>();
    parametros.forEach((key, value) -> {
      if (
        !key.equals("pagina") && !key.equals("tamano") && !value.isBlank()
      ) filtros.put(key, value.strip());
    });
    return repository.listar(dominio, filtros, pagina, tamano);
  }

  public void validar(String dominio, Map<String, String> parametros) {
    Set<String> permitidos = new HashSet<>(Set.of("q", "pagina", "tamano"));
    permitidos.addAll(
      switch (dominio) {
        case "programas" -> Set.of("universidad", "universidadId");
        case "residentes" -> Set.of(
          "universidad",
          "programa",
          "estado",
          "periodo"
        );
        case "usuarios" -> Set.of("rol", "estado");
        case "servicios", "universidades" -> Set.of("estado");
        case "historial" -> Set.of("modulo", "accion", "desde", "hasta");
        case "rotaciones" -> Set.of(
          "universidad",
          "servicio",
          "estado",
          "periodo",
          "proximas"
        );
        default -> throw error("Listado no disponible.");
      }
    );
    if (!permitidos.containsAll(parametros.keySet())) throw error(
      "Uno de los filtros no está disponible para este listado."
    );
    int pagina = numero(parametros.getOrDefault("pagina", "1"), 1, 1000000);
    int tamano = numero(parametros.getOrDefault("tamano", "10"), 1, 100);
    Map<String, String> filtros = new LinkedHashMap<>();
    parametros.forEach((key, value) -> {
      if (key.equals("pagina") || key.equals("tamano")) return;
      String limpio = value.strip();
      if (limpio.length() > 200) throw error(
        "Los filtros admiten hasta 200 caracteres."
      );
      if (!limpio.isEmpty()) filtros.put(key, limpio);
    });
    if (filtros.containsKey("universidadId")) numero(
      filtros.get("universidadId"),
      1,
      Integer.MAX_VALUE
    );
    String periodo = filtros.get("periodo");
    if (periodo != null && !periodo.matches("[0-9]{4}-[12]")) throw error(
      "El periodo debe tener formato AAAA-1 o AAAA-2."
    );
    try {
      LocalDate desde = filtros.containsKey("desde")
        ? LocalDate.parse(filtros.get("desde"))
        : null;
      LocalDate hasta = filtros.containsKey("hasta")
        ? LocalDate.parse(filtros.get("hasta"))
        : null;
      if (desde != null && hasta != null && desde.isAfter(hasta)) throw error(
        "La fecha inicial no puede superar la fecha final."
      );
    } catch (java.time.format.DateTimeParseException e) {
      throw error("Usa fechas válidas con formato AAAA-MM-DD.");
    }
    if (
      filtros.containsKey("proximas") && !filtros.get("proximas").equals("true")
    ) throw error("El filtro próximas admite true.");
  }

  public Map<String, List<String>> catalogos() {
    return repository.catalogos();
  }

  private int numero(String valor, int min, int max) {
    try {
      int n = Integer.parseInt(valor);
      if (n >= min && n <= max) return n;
    } catch (NumberFormatException e) {}
    throw error("Revisa la página (1 a 1000000) y el tamaño (1 a 100).");
  }

  private NegocioException error(String mensaje) {
    return new NegocioException(HttpStatus.BAD_REQUEST, mensaje);
  }
}
