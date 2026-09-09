package co.edu.javeriana.husi.dto.request;
import jakarta.validation.constraints.*;
public record ProgramaRequest(
 @NotBlank(message="Escribe el nombre del programa.") @Size(max=150,message="El nombre admite hasta 150 caracteres.") String nombre,
 @NotNull(message="Selecciona una universidad.") @Positive(message="Selecciona una universidad válida.") Integer universidadId
) {}
