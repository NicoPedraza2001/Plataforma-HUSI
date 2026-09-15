package co.edu.javeriana.husi.dto.request;
import jakarta.validation.constraints.*;
public record UniversidadRequest(
 @NotBlank(message="Escribe el nombre de la universidad.") @Size(max=150,message="El nombre admite hasta 150 caracteres.") String nombre,
 @NotBlank(message="Escribe el país.") @Size(max=100,message="El país admite hasta 100 caracteres.") String pais,
 @Size(max=50,message="El perfil admite hasta 50 caracteres.") String perfil
) {}
