package co.edu.javeriana.husi.exception;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

  public record ErrorResponse(String mensaje, Map<String, String> campos) {}

  @ExceptionHandler(NegocioException.class)
  public ResponseEntity<ErrorResponse> negocio(NegocioException e) {
    return ResponseEntity.status(e.getStatus()).body(
      new ErrorResponse(e.getMessage(), Map.of())
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validacion(
    MethodArgumentNotValidException e
  ) {
    Map<String, String> campos = new LinkedHashMap<>();
    e.getBindingResult()
      .getFieldErrors()
      .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(
      new ErrorResponse("Revisa los datos del formulario.", campos)
    );
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
  })
  public ResponseEntity<ErrorResponse> formato(Exception e) {
    return ResponseEntity.badRequest().body(
      new ErrorResponse("El formato de los datos no es válido.", Map.of())
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> integridad(
    DataIntegrityViolationException e
  ) {
    Throwable causa = e;
    while (
        causa.getCause() != null && !(causa instanceof java.sql.SQLException)
      )
      causa = causa.getCause();
    String estado =
      causa instanceof java.sql.SQLException sql ? sql.getSQLState() : "";
    if (
      Set.of("23502", "23514", "22001", "22003", "22018").contains(
        estado == null ? "" : estado
      )
    ) {
      return ResponseEntity.badRequest().body(
        new ErrorResponse(
          "Revisa los campos obligatorios, los tipos, las longitudes y los valores permitidos.",
          Map.of()
        )
      );
    }
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
      new ErrorResponse(
        "No se pudo guardar: existe un registro duplicado o una referencia dejó de estar disponible. Actualiza el listado e intenta de nuevo.",
        Map.of()
      )
    );
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ResponseEntity<ErrorResponse> baseDeDatos(
    org.springframework.dao.DataAccessException e
  ) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
      new ErrorResponse(
        "No se pudo consultar la base de datos. Intenta nuevamente en unos momentos.",
        Map.of()
      )
    );
  }
}
