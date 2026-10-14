package co.edu.javeriana.husi.dto.response;

/** Datos del usuario en sesión. Nunca incluye la contraseña ni su hash. */
public record PerfilResponse(String nombres, String apellidos, String correo, String rol) {}
