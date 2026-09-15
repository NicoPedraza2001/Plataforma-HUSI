package co.edu.javeriana.husi.dto.request;
import jakarta.validation.constraints.NotNull;
public record EstadoRotacionRequest(@NotNull Boolean activa) {}
