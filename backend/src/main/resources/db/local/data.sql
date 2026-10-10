-- Datos ficticios exclusivos de H2 para recorrer los módulos.
INSERT INTO rol(nombre_rol) VALUES ('Administrador'),('Coordinador');
-- Firma el historial de cambios mientras no exista inicio de sesión (husi.auditoria.correo-usuario-sin-sesion).
INSERT INTO usuario(id_rol,nombre,apellido,correo,contrasena) VALUES (1,'Administrador','Sistema','admin@husi.com','$2a$10$REEMPLAZAR_POR_HASH_BCRYPT_REAL');
INSERT INTO universidad(nombre_universidad,pais) VALUES ('Universidad de demostración','Colombia');
INSERT INTO programa(id_universidad,nombre_programa) VALUES (1,'Convenio Nacional');
INSERT INTO servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) VALUES ('Servicio de demostración',3,2);
INSERT INTO asignatura(id_servicio,nombre_asignatura) VALUES (1,'Asignatura de demostración');
INSERT INTO tipo_documento(codigo,nombre) VALUES ('CC','Cédula de ciudadanía'),('TI','Tarjeta de identidad'),('CE','Cédula de extranjería'),('PA','Pasaporte'),('RUT','Registro único tributario'),('PEP','Permiso especial de permanencia'),('PPT','Permiso por protección temporal');
INSERT INTO perfil_academico(nombre) VALUES ('Pregrado'),('Posgrado');
INSERT INTO residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) VALUES (1,1,'Residente de demostración','LOCAL-001');
