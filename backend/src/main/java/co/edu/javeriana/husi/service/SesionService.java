package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.InicioSesionRequest;
import co.edu.javeriana.husi.dto.response.PerfilResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SesionService {
  /** Comprueba correo y contraseña y guarda el usuario en una sesión nueva. */
  PerfilResponse iniciarSesion(
    InicioSesionRequest credenciales,
    HttpServletRequest request,
    HttpServletResponse response
  );

  /** Invalida la sesión en el servidor y borra su cookie. */
  void cerrarSesion(HttpServletRequest request, HttpServletResponse response);

  PerfilResponse perfil();
}
