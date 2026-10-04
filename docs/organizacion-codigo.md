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
