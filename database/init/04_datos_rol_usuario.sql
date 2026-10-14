-- ============================================================
-- Datos: roles y usuario administrador inicial
-- Generado a partir de docs/database/04_Datos_Rol_Usuario.md
-- Se ejecuta automaticamente al levantar el contenedor de
-- PostgreSQL por primera vez (docker-entrypoint-initdb.d).
-- ============================================================

-- La contrasena del administrador es un marcador de posicion a proposito:
-- ninguna contrasena ni hash real se guarda en el repositorio. Al arrancar,
-- el backend (security/InicializadorContrasenaAdministrador.java) guarda el
-- hash BCrypt de la variable de entorno HUSI_ADMIN_CONTRASENA_INICIAL.

INSERT INTO rol (nombre_rol) VALUES
    ('Administrador'),
    ('Coordinador');

INSERT INTO usuario (id_rol, nombre, apellido, correo, contrasena) VALUES
    (1, 'Administrador', 'Sistema', 'admin@husi.com', '$2a$10$REEMPLAZAR_POR_HASH_BCRYPT_REAL');

