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
  public ResponseEntity<ErrorResponse> manejarErrorDeNegocio(
    NegocioException excepcion
  ) {
    return ResponseEntity.status(excepcion.getStatus()).body(
      new ErrorResponse(excepcion.getMessage(), Map.of())
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> manejarErrorDeValidacion(
    MethodArgumentNotValidException excepcion
  ) {
    Map<String, String> campos = new LinkedHashMap<>();
    excepcion
      .getBindingResult()
      .getFieldErrors()
      .forEach(errorCampo ->
        campos.putIfAbsent(errorCampo.getField(), errorCampo.getDefaultMessage())
      );
    return ResponseEntity.badRequest().body(
      new ErrorResponse("Revisa los datos del formulario.", campos)
    );
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
  })
  public ResponseEntity<ErrorResponse> manejarErrorDeFormato(Exception excepcion) {
    return ResponseEntity.badRequest().body(
      new ErrorResponse("El formato de los datos no es válido.", Map.of())
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> manejarErrorDeIntegridad(
    DataIntegrityViolationException excepcion
  ) {
    // Busca la SQLException original para distinguir datos inválidos de duplicados.
    Throwable causa = excepcion;
    while (
        causa.getCause() != null && !(causa instanceof java.sql.SQLException)
      )
      causa = causa.getCause();
    String codigoSql =
      causa instanceof java.sql.SQLException errorSql ? errorSql.getSQLState() : "";
    if (
      Set.of("23502", "23514", "22001", "22003", "22018").contains(
        codigoSql == null ? "" : codigoSql
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
  public ResponseEntity<ErrorResponse> manejarErrorDeBaseDeDatos(
    org.springframework.dao.DataAccessException excepcion
  ) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
      new ErrorResponse(
        "No se pudo consultar la base de datos. Intenta nuevamente en unos momentos.",
        Map.of()
      )
    );
  }
}
