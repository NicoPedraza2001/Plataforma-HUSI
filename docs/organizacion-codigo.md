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

## Importación de Excel (carga inicial)

Vive en `features/admin/rotaciones/importar-rotaciones/` y se abre desde «Importar Excel» en la barra lateral (`/admin/importar-excel`). Es un asistente por pasos que sigue el diagrama entidad-relación, para corregir cada error en su paso antes de guardar:

| Paso | Carpeta | Qué resuelve |
|---|---|---|
| 1. Archivo | `paso-archivo/` | Lee el .xlsx en el navegador. |
| 2. Columnas | `paso-columnas/` | Relaciona columnas o valores comunes con cada campo (`campos-carga.ts`). |
| 3. Universidades | `paso-universidades/` | Crea nuevas, asocia mal escritas a una existente, activa inactivas o excluye filas. |
| 4. Programas | `paso-programas/` | Lo mismo, buscando cada programa dentro de su universidad. |
| 5. Servicios | `paso-servicios/` | Crea nuevos con sus cupos, asocia mal escritos, usa los cupos del sistema si no coinciden, activa inactivos o excluye filas. |
| 6. Asignaturas | `paso-asignaturas/` | Lo mismo que Programas, buscando cada asignatura dentro de su servicio. |
| 7. Residentes | `paso-residentes/` | Busca por documento (`POST /residentes/buscar-por-documentos`); crea, actualiza o usa los datos del sistema. Tipo de documento, perfil y programa se eligen de los catálogos. |
| 8. Rotaciones | `paso-rotaciones/` | Prioridad y corrección de fechas; valida todo contra el backend sin guardar. |
| 9. Confirmar | `paso-confirmacion/` | Vista previa de cada asignación (`vista-previa-carga/`) y guardado con la huella de la última validación. |

Todo lo que falta se crea en su paso con revisión del administrador; por eso la validación final se envía con `crearFaltantes=false` (`CREAR_FALTANTES_EN_LA_VALIDACION`) y cualquier faltante aparece como error que lleva a su paso.

- `importar-rotaciones.ts` solo orquesta: guarda las filas (única fuente de verdad), habilita cada paso cuando el anterior no tiene pendientes y conecta los eventos.
- Cada paso de emparejamiento separa la lógica pura (`emparejar-universidades.ts`, `emparejar-programas.ts`, `emparejar-servicios.ts`, `emparejar-asignaturas.ts`, `revisar-residentes.ts`) del componente, que llama al backend y reescribe las filas.
- Piezas compartidas entre pasos: `emparejamiento.ts` (comparación igual a la del backend, sugerencias por similitud, reescritura de filas, creación en lote y concordancia singular/plural), `filtro-de-tabla.ts`, `tabla-emparejamiento/`, `confirmar-exclusion/`, `resultado-carga/` (los errores llevan al paso donde se corrigen) y `pasos.css`.
- Los formularios de creación reutilizan los de cada módulo (`formulario-universidad`, `formulario-programa`, `formulario-servicio`, `formulario-asignatura`, `formulario-residente`).

## Historial de auditoría

Cada cambio que hace un administrador queda en `historial_auditoria` con quién lo hizo, cuándo, el módulo, la acción (`CREÓ`, `ACTUALIZÓ`, `ACTIVÓ`, `INACTIVÓ`, `ELIMINÓ`, `ASIGNÓ`, `CAMBIÓ ESTADO`, `TRASLADÓ`, `IMPORTÓ`), el registro afectado y una fotografía JSON del registro antes (`datos_anteriores`) y después (`datos_nuevos`). La pantalla `/admin/historial` lista los movimientos y «Ver detalle» abre `detalle-movimiento/` con la comparación campo por campo.

- Cada servicio toma la fotografía con `FotografiaAuditoriaMapper` **antes** de modificar la entidad y llama a `HistorialAuditoriaService.registrar(...)` después de guardar, dentro de la misma transacción: si el cambio se revierte, su registro también. Guardar un formulario sin cambios no deja movimiento. La contraseña de un usuario nunca se guarda; solo consta «Se cambió».
- **Importaciones**: una importación ocupa una sola fila (`lote_importacion`) con el resumen («Residentes creados: 120», «Filas omitidas: 3»…), no una por registro. La importación de programas y la confirmación de la carga inicial lo hacen con `ejecutarComoImportacion`. El asistente de Excel crea registros con las rutas individuales, así que `LoteImportacion` + `loteImportacionInterceptor` envían `X-Lote-Importacion` (y el nombre del archivo) mientras está abierto, y `LoteImportacionFiltro` los agrupa en la misma fila. Si el asistente se abandona antes de confirmar, la fila queda con estado «En curso».
- **Usuario responsable**: firma el usuario de la sesión iniciada (`security/UsuarioEnSesion`). Cada fila guarda además su nombre y correo (`nombre_responsable`, `correo_responsable`), tal como estaban en ese momento: al eliminar la cuenta, `id_usuario` queda en null y el movimiento conserva quién lo hizo.
- **Eliminar usuarios**: se rechaza si coordina un servicio o si es la cuenta en sesión; sus movimientos no impiden eliminarlo.
- Bases PostgreSQL creadas antes de este cambio deben ejecutar `database/migrations/20261010_historial_auditoria_detalle.sql` y `database/migrations/20261104_historial_conserva_responsable.sql`; sin esas columnas Hibernate (`ddl-auto: validate`) no arranca.

## Inicio de sesión y seguridad

- **Backend** (`security/`): `ConfiguracionSeguridad` define las reglas. `POST /sesion/iniciar` es la única ruta abierta; `/sesion/**` exige sesión y el resto de la API, rol Administrador. Para abrir rutas al coordinador, agregarlas ahí.
- **Sesión en el servidor**: al iniciar sesión, `SesionServiceImpl` crea una sesión nueva (id distinto, contra fijación de sesión) y el navegador solo recibe la cookie `HUSI_SESION` (HttpOnly, SameSite=Strict, Secure fuera de dev, 30 min de inactividad). El frontend nunca ve un token ni la contraseña después de enviarla.
- **Sesión vigente**: `SesionVigenteFiltro` compara en cada petición la sesión con la cuenta en la base. Si la cuenta se eliminó, se desactivó, cambió de rol o cambió de correo o contraseña, la sesión termina y el backend responde 401. Cambiar el correo o la contraseña en Ajustes cierra además la sesión actual (`PATCH /sesion/credenciales` responde 204) y el frontend lleva al login.
- **CSRF**: la cookie `XSRF-TOKEN` (ruta `/`) se copia en la cabecera `X-XSRF-TOKEN`; Angular lo hace solo en POST/PUT/PATCH/DELETE.
- **Contraseñas**: BCrypt costo 12 (`PasswordEncoder` único, también lo usa `UsuarioServiceImpl`). Mismo mensaje para correo inexistente, contraseña errada o usuario inactivo. `LimiteIntentosInicioSesion` bloquea un correo 15 min tras 5 fallos.
- **Administrador inicial**: los scripts guardan un marcador en vez de un hash. Al arrancar, `InicializadorContrasenaAdministrador` guarda el hash de la variable de entorno `HUSI_ADMIN_CONTRASENA_INICIAL` si `admin@husi.com` aún no tiene contraseña; nunca sobrescribe una existente.
- **Frontend**: `services/sesion-actual.ts` guarda el perfil en un signal; `services/sesion.guard.ts` (`requiereRol`) protege `/admin`; `services/sesion-expirada.interceptor.ts` lleva al login ante un 401. El panel de cada rol está en `PANEL_POR_ROL` (el coordinador todavía no tiene).
- **Tests**: los de servicios usan `@WithUserDetails("admin@husi.com")`; los que llaman por HTTP usan `ClienteApiDePrueba`, que inicia sesión y envía el token CSRF. `SesionH2IntegrationTests` cubre 401/403, CSRF, bloqueo y cierre de sesión.

## Dónde agregar cambios

Una pantalla nueva pertenece a su módulo en `features`; un formulario específico permanece en ese módulo. Extraer a `shared` solamente lo que se comparte. Agregar llamadas de red al servicio del dominio y contratos a `models`. En backend, conservar controller → service → repository y evitar duplicar consultas o reglas en los controladores.

## Verificación y ejecución

Desde `frontend`: `npm start -- --port 4200`, `npm run build`, `npm test -- --watch=false` y `npm run format:check`.

Desde `backend`: `./mvnw.cmd spring-boot:run` y `./mvnw.cmd test`. Para iniciar sesión en local, definir antes `HUSI_ADMIN_CONTRASENA_INICIAL` (o `husi.seguridad.administrador-inicial.contrasena` en `application-personal.yml`, ignorado por git, con el perfil `local,personal`). Las pruebas de integración requieren PostgreSQL disponible con la configuración local del proyecto.

Las pruebas de los CRUD de residentes y usuarios se describen en residentes-usuarios.md.

## Referencias

- https://github.com/RoberthMendez/Biskit/tree/main/Biskit
- https://github.com/RoberthMendez/BiskitServer

Se reutiliza la idea de organización, no se copia la arquitectura de autenticación ni código ajeno al dominio HUSI.

El CRUD e importador de Rotaciones se retiraron; se conservan datos y lecturas históricas del dashboard. Detalles actuales en [residentes-usuarios.md](residentes-usuarios.md).
