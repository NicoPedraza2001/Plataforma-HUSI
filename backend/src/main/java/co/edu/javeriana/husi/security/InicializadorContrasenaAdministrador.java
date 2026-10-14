package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna la primera contraseña del administrador sin escribirla nunca en el repositorio.
 *
 * <p>Los scripts de {@code database/init} crean {@code admin@husi.com} con un marcador de
 * posición en vez de un hash. Al arrancar, si ese usuario todavía no tiene un hash BCrypt válido
 * y existe la variable de entorno {@code HUSI_ADMIN_CONTRASENA_INICIAL}, se guarda su hash.
 * Si el administrador ya tiene contraseña, no se toca: se cambia desde la pantalla de Usuarios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InicializadorContrasenaAdministrador implements ApplicationRunner {

  private static final Pattern HASH_BCRYPT = Pattern.compile(
    "^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$"
  );
  private static final int LONGITUD_MINIMA = 8;
  private static final int BYTES_MAXIMOS_BCRYPT = 72;

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder codificadorContrasena;

  @Value("${husi.seguridad.administrador-inicial.correo}")
  private String correoAdministrador;

  @Value("${husi.seguridad.administrador-inicial.contrasena}")
  private String contrasenaInicial;

  @Override
  @Transactional
  public void run(ApplicationArguments argumentos) {
    usuarioRepository
      .findByCorreoIgnoreCase(correoAdministrador)
      .filter(administrador -> !HASH_BCRYPT.matcher(administrador.getContrasena()).matches())
      .ifPresent(administrador -> {
        if (!esContrasenaValida(contrasenaInicial)) {
          log.warn(
            "El usuario {} no tiene contraseña y no podrá iniciar sesión. Define la variable de " +
              "entorno HUSI_ADMIN_CONTRASENA_INICIAL (8 caracteres o más) y reinicia el backend.",
            correoAdministrador
          );
          return;
        }
        administrador.setContrasena(codificadorContrasena.encode(contrasenaInicial));
        usuarioRepository.save(administrador);
        log.info("Se asignó la contraseña inicial del usuario {}.", correoAdministrador);
      });
  }

  private boolean esContrasenaValida(String contrasena) {
    return (
      contrasena != null &&
      !contrasena.isBlank() &&
      contrasena.length() >= LONGITUD_MINIMA &&
      contrasena.getBytes(StandardCharsets.UTF_8).length <= BYTES_MAXIMOS_BCRYPT
    );
  }
}
