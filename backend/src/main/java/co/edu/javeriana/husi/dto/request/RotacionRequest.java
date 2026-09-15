package co.edu.javeriana.husi.dto.request;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record RotacionRequest(@NotNull @Positive Integer asignaturaId, @NotNull LocalDate inicio, @NotNull LocalDate fin) {}
