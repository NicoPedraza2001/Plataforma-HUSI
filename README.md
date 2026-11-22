# Plataforma HUSI

Sistema de Información para el Apoyo a la Gestión de Rotaciones de residentes del
**Hospital Universitario San Ignacio**.

## Contenido

1. [Arquitectura y despliegue](#arquitectura-y-despliegue)
2. [Estructura del repositorio](#estructura-del-repositorio)
3. [Ejecución local](#ejecución-local)
4. [Pruebas y formato](#pruebas-y-formato)
5. [Frontend](#frontend)
6. [Backend](#backend)
7. [Seguridad](#seguridad)
8. [Diseño visual](#diseño-visual)
9. [Documentación adicional](#documentación-adicional)

## Arquitectura y despliegue

| Capa | Tecnología | Despliegue |
|---|---|---|
| Frontend | Angular 21 (standalone, signals) | Vercel (`frontend/vercel.json`) |
| Backend | Spring Boot 4 · Java 21 (API REST, `context-path: /api`) | Render, Docker (`render.yaml`) |
| Base de datos | PostgreSQL 18 | Docker en local · Neon en producción |

En producción, Vercel reenvía `/api/*` al backend de Render, así que el navegador solo habla
con un origen. En desarrollo, `frontend/proxy.conf.json` hace lo mismo hacia `localhost:8080`.

**Publicar el frontend** (la primera vez requiere `npx vercel login` y
`npx vercel link --project plataforma-husi` dentro de `frontend/`):

```bash
sh frontend/scripts/desplegar-vercel.sh
```

El backend se redespliega solo en Render con cada push a `main` (`autoDeploy: true`). Sus
secretos (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`,
`HUSI_ADMIN_CONTRASENA_INICIAL`) se configuran en el panel de Render, nunca en el repositorio.

## Estructura del repositorio

```
frontend/            Aplicación web Angular
backend/             API REST Spring Boot
database/            Scripts SQL de creación y carga inicial (init/) y pgAdmin
docs/                Documentación funcional, de base de datos y planes de prueba
docker-compose.yml   PostgreSQL + pgAdmin + backend para desarrollo
render.yaml          Blueprint del backend en Render
```

## Ejecución local

Requisitos: Docker, Node.js + npm y, opcionalmente, Java 21 para correr el backend fuera de
Docker.

1. Crear en la raíz un archivo `.env` (ignorado por git) con la contraseña inicial del
   administrador `admin@husi.com` (mínimo 8 caracteres; el backend guarda solo su hash
   BCrypt). Debe contener una sola línea:

   ```bash
   HUSI_ADMIN_CONTRASENA_INICIAL=tu-contraseña
   ```

2. Levantar base de datos, pgAdmin y backend:

   ```bash
   docker compose up -d --build
   ```

3. Levantar el frontend en <http://localhost:4200>:

   ```bash
   cd frontend && npm install && npm start
   ```

| Servicio | Dirección |
|---|---|
| API | <http://localhost:8080/api> |
| Swagger UI | <http://localhost:8080/api/swagger-ui.html> |
| PostgreSQL | `localhost:5432` — base `husi`, usuario `husi`, contraseña `husi_dev` |
| pgAdmin | <http://localhost:5050> — `admin@husi.com` / `admin_dev` |

Sin Docker se puede usar el perfil `local` con H2: ver
[validaciones y perfil local](docs/validaciones-y-perfil-local.md). Detalle de la base y de la
carga inicial en [database/README.md](database/README.md).

## Pruebas y formato

```bash
cd frontend && npm test                                      # Vitest + Angular TestBed
cd backend && mvnw.cmd clean test                            # JUnit (requiere PostgreSQL)
cd backend && mvnw.cmd test "-Dtest=*H2IntegrationTests"     # Solo H2, sin Docker
cd frontend && npm run format:check                          # Prettier (TS/HTML/CSS + Java)
```

El plan de pruebas por caso de uso está en
[docs/pruebas/plan-pruebas-junit.md](docs/pruebas/plan-pruebas-junit.md).

## Frontend

Angular 21 con componentes standalone (sin `NgModule`), estado con signals (`signal`,
`computed`) y control de flujo nativo (`@if`, `@for`, `@empty`).

```
frontend/src/app/
  layout/
    admin/admin-layout/      Shell del panel admin: sidebar + <router-outlet>
    public/public-layout/    Shell público: header + footer
  features/<dominio>/<módulo>/<pantalla>/
    <pantalla>.ts | .html | .css | .spec.ts
  models/                    Interfaces de los DTO del backend
  services/                  Clientes HTTP, uno por controller
  shared/                    Paginacion, FocoModal, excel/, consulta/, formularios/
  app.routes.ts              Único árbol de rutas
```

### Convenciones

- Cada pantalla vive en su carpeta con sus 4 archivos; no hay barrels (`index.ts`).
- Clase = PascalCase de la carpeta, sin sufijo `Component` (`listado-programas/` →
  `ListadoProgramas`). Selector = `app-` + kebab-case de la carpeta.
- Los miembros usados solo por la plantilla son `protected`.
- Cada componente tiene su `*.spec.ts` con, como mínimo, `should create`.
- Toda importación/exportación de Excel usa `shared/excel` (`leerExcel`, `descargarLibro`),
  nunca `exceljs` directamente. La lógica propia de una pantalla (p. ej. el mapeo de columnas)
  queda en su carpeta.
- Los listados consultan el backend; no se usan datos DEMO. Si falta un endpoint, se muestra un
  estado vacío o de error explícito.
- Patrón de página admin (referencia: `features/admin/programas/listado-programas`):
  `.page-head` con título, breadcrumb y acción primaria; `.card`; `.search`; tabla y
  `.paginacion`. Las pantallas de `/admin/*` no repiten el sidebar.

### Rutas

Todo el árbol está en `app.routes.ts`, sin lazy loading. Las pantallas de
`features/admin/...` cuelgan de `AdminLayout` (`/admin/*`, protegido por
`requiereRol(ROL_ADMINISTRADOR)`); las de `features/public/...` y `features/auth/...`, de
`PublicLayout`.

### Conexión con el backend

Los componentes nunca usan `HttpClient` directamente: inyectan un servicio de `services/`, que
es espejo de un controller del backend.

| Frontend | Backend |
|---|---|
| `programa.service.ts` · `programa.model.ts` | `ProgramaController` (`/api/programas`) |
| `universidad.service.ts` · `universidad.model.ts` | `UniversidadController` (`/api/universidades`) |
| `sesion.service.ts` (+ `sesion-actual.ts`, `sesion.guard.ts`) | `SesionController` (`/api/sesion`) + `security/` |
| `coordinador.service.ts` · `coordinador.model.ts` | `CoordinadorController` (`/api/coordinador`) |
| `api-error.ts` · `api-error.model.ts` | `exception/ApiExceptionHandler` |

Para conectar un módulo nuevo:

1. `models/<recurso>.model.ts`: una interfaz por DTO, comentando el DTO de origen
   (`/** Backend: dto/response/XResponse. */`).
2. `services/<recurso>.service.ts` (`providedIn: 'root'`), URL `` `${API_URL}/<recurso>` `` y los
   mismos nombres de método que el controller, cada uno con su verbo y ruta comentados.
3. `<recurso>.service.spec.ts` con `provideHttpClientTesting()` comprobando verbo, URL y body.
4. En el componente: `inject(...)`, `await firstValueFrom(...)` en `try/catch` y
   `mensajeError(e)` en un signal de error.
5. En el spec del componente: `{ provide: XService, useValue: mock }`.

## Backend

Paquete raíz `co.edu.javeriana.husi`. Cada petición sigue el camino
`controller → service (interfaz) → service/impl → repository`, con `dto/request`,
`dto/response`, `mapper` y `model/entity`. Nunca se expone una entidad JPA.

El esquema lo crean los scripts de `database/init` (Hibernate corre con `ddl-auto: validate`).
Perfiles: `dev` (local con Docker), `local` (H2) y `prod` (variables de entorno). Más detalle
en [backend/README.md](backend/README.md).

## Seguridad

- Toda la API exige sesión: cookie HttpOnly `HUSI_SESION` + token CSRF. La sesión vive en el
  servidor (no JWT).
- Roles: Administrador (panel `/admin`) y Coordinador (`/api/coordinador`).
- Nunca se guardan tokens ni contraseñas en `localStorage`, ni contraseñas en el repositorio.

## Diseño visual

Todo el estilo usa variables CSS de `frontend/src/styles.css`; los componentes no usan valores
hex sueltos.

| Variable | Uso |
|---|---|
| `--husi-navy` / `--husi-navy-deep` / `--husi-blue` / `--husi-deep` | Marca, títulos, hover y degradados |
| `--husi-teal` / `--husi-teal-dark` | Acento, focus rings, badges positivos |
| `--husi-green` / `--husi-green-dark` | Acción primaria del panel admin |
| `--husi-red` / `--husi-red-dark` | Acción destructiva, badges negativos |
| `--husi-bg` / `--husi-surface` / `--husi-border` | Fondos y bordes |
| `--husi-text` / `--husi-muted` | Texto principal y secundario |
| `--font-serif` (Playfair Display) / `--font-sans` (Inter) | Títulos / cuerpo |
| `--radius-*` / `--shadow-*` / `--sidebar-width` | Bordes, sombras y ancho del sidebar |

Al traer pantallas de Figma se prioriza la consistencia con estas variables sobre el hex exacto
del diseño.

## Documentación adicional

- [Organización del código](docs/organizacion-codigo.md)
- [Integración frontend–backend](docs/integracion-frontend-backend.md)
- [Validaciones y perfil local](docs/validaciones-y-perfil-local.md)
- [Carga inicial desde Excel](docs/carga-inicial-excel.md)
- Módulos: [universidades y programas](docs/universidades-programas.md),
  [servicios](docs/servicios.md), [residentes y usuarios](docs/residentes-usuarios.md),
  [asignaturas y coordinadores](docs/asignaturas-coordinadores.md),
  [rotaciones](docs/rotaciones.md)
- [Base de datos](docs/database/) · [Plan de pruebas](docs/pruebas/plan-pruebas-junit.md)
