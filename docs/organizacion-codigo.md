# Organización del código HUSI

La organización conserva Angular standalone y las capas existentes de Spring Boot. Se toma como referencia la separación de pantallas, formularios y servicios de Biskit y las capas de BiskitServer, adaptadas a los módulos actuales de HUSI.

## Frontend

- `src/app/features/`: pantallas agrupadas por área y módulo. Cada pantalla tiene sus archivos TypeScript, HTML y CSS.
- `features/admin/programas/formulario-programa/`, `universidades/formulario-universidad/` `residentes/formulario-residente/` y `usuarios/formulario-usuario/`: formularios reutilizables dentro de cada módulo. Reciben datos y emiten guardar/cancelar; el listado coordina la operación con el servicio.
- `src/app/layout/`: estructura visual de administración y del sitio público.
- `src/app/models/`: contratos de datos por dominio, paginación y errores.
- `src/app/services/`: llamadas HTTP por dominio, con rutas y respuestas Observable explícitas. Los componentes consumen estos servicios.
- `src/app/shared/consulta/`: carga, búsqueda y paginación de listados mediante consultas al backend.
- `src/app/shared/excel/`: lectura y descarga de Excel compartidas; el mapeo específico permanece en cada importador.
- `src/app/shared/formularios/`: estilos comunes de formularios.
- `src/app/shared/`: paginación, foco de modales y otras piezas comunes.

Las consultas de listados provienen del backend. Las filas de previsualización de un Excel pertenecen al archivo elegido y no sustituyen los datos persistidos.

## Backend

- `controller/`: rutas HTTP y validación de entrada.
- `dto/request/` y `dto/response/`: contratos de entrada y salida; residentes y usuarios tienen contratos específicos; las respuestas de usuario excluyen la contraseña.
- `service/` y `service/impl/`: contratos y reglas de negocio.
- `repository/`: acceso a datos; `support/ConsultaSql` concentra la ejecución común y `query/ListadoSql` las consultas de listados.
- `mapper/`: conversión de entidades a respuestas.
- `model/entity/`: entidades persistidas.
- `exception/`: traducción de errores a respuestas HTTP.

No se cambió el esquema de base de datos ni las reglas de activación, eliminación o asociación por esta reorganización.

## Dónde agregar cambios

Una pantalla nueva pertenece a su módulo en `features`; un formulario específico permanece en ese módulo. Extraer a `shared` solamente lo que se comparte. Agregar llamadas de red al servicio del dominio y contratos a `models`. En backend, conservar controller → service → repository y evitar duplicar consultas o reglas en los controladores.

## Verificación y ejecución

Desde `frontend`: `npm start -- --port 4200`, `npm run build`, `npm test -- --watch=false` y `npm run format:check`.

Desde `backend`: `./mvnw.cmd spring-boot:run` y `./mvnw.cmd test`. Las pruebas de integración requieren PostgreSQL disponible con la configuración local del proyecto.

Las pruebas de los CRUD de residentes y usuarios se describen en residentes-usuarios.md.

## Referencias

- https://github.com/RoberthMendez/Biskit/tree/main/Biskit
- https://github.com/RoberthMendez/BiskitServer

Se reutiliza la idea de organización, no se copia la arquitectura de autenticación ni código ajeno al dominio HUSI.

El CRUD e importador de Rotaciones se retiraron; se conservan datos y lecturas históricas del dashboard. Detalles actuales en [residentes-usuarios.md](residentes-usuarios.md).
