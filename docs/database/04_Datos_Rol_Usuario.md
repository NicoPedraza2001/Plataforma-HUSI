# Datos — Rol y usuario inicial

Primer archivo de datos. No proviene del Excel: son los registros mínimos
para poder entrar al sistema y para que la auditoría tenga a quién apuntar.

Reemplaza el hash de `contrasena` por uno real generado con
`BCryptPasswordEncoder` antes de probar el login.

```sql
INSERT INTO rol (nombre_rol) VALUES
    ('Administrador'),
    ('Coordinador');

INSERT INTO usuario (id_rol, nombre, apellido, correo, contrasena) VALUES
    (1, 'Administrador', 'Sistema', 'admin@husi.com', '$2a$10$REEMPLAZAR_POR_HASH_BCRYPT_REAL');
```

---

## Verificación

```sql
SELECT r.nombre_rol, u.correo, u.estado
FROM usuario u JOIN rol r ON r.id_rol = u.id_rol;
```
