package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.ActualizacionCredencialesRequest;
import co.edu.javeriana.husi.dto.request.ActualizacionPerfilRequest;
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

  /** Cambia solo los nombres y/o apellidos que lleguen; deja constancia en el historial. */
  PerfilResponse actualizarPerfil(ActualizacionPerfilRequest request);

  /**
   * Cambia el correo y/o la contraseña tras comprobar la contraseña actual. Los fallos cuentan
   * para el bloqueo por intentos, igual que en el inicio de sesión. Quien llama debe cerrar la
   * sesión después: los datos nuevos solo valen tras ingresar otra vez.
   */
  void actualizarCredenciales(ActualizacionCredencialesRequest request);
}
