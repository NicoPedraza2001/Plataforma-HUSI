package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;

/** Documentos a buscar en bloque; lo usa la importación de Excel para no consultar uno a uno. */
public record BusquedaDocumentosRequest(
  @NotNull
  @Size(max = 5000, message = "Busca hasta 5000 documentos por consulta.")
  List<@NotBlank @Size(max = 50) String> documentos
) {}
