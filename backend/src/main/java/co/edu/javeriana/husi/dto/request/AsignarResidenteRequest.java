package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record AsignarResidenteRequest(@NotNull @Positive Integer residenteId) {}
