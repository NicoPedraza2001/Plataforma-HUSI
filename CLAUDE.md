# Plataforma HUSI

Plataforma de gestión de residentes del Hospital Universitario San Ignacio.

## Stack

- **Frontend**: Angular 21 (standalone components, sin `NgModule`), en `/frontend`.
- **Backend**: Spring Boot (API REST), en `/backend`. Capas: controller → service (interfaz)
  → service/impl → repository, con `dto/request`, `dto/response`, `mapper` y `model/entity`.
- **Gestor de paquetes**: npm.
- **Tests**: Vitest + Angular TestBed (`ng test`). Cada componente trae su `*.spec.ts` con,
  como mínimo, el test `should create`.
- **Formato**: Prettier (`printWidth: 100`, comillas simples, parser `angular` para `.html`).

## Estructura del frontend (`frontend/src/app`)

```
app/
  layout/
    admin/admin-layout/      # Shell del panel admin: sidebar + <router-outlet>
    public/public-layout/    # Shell público: header + footer
  features/
    <dominio>/<módulo>/<pantalla>/
      <pantalla>.ts
      <pantalla>.html
      <pantalla>.css
      <pantalla>.spec.ts
  models/                     # Interfaces de los DTO del backend (≈ backend dto/)
    <recurso>.model.ts
  services/                   # Clientes HTTP, uno por controller (≈ backend controller/)
    api.config.ts             # API_URL = '/api'
    api-error.ts              # mensajeError(): error HTTP → texto para el usuario
    <recurso>.service.ts
    <recurso>.service.spec.ts
  shared/                     # Piezas reutilizables (paginacion/, foco-modal/, excel/)
  app.routes.ts               # Único árbol de rutas de la app
```

- `features/admin/...` cuelga de `AdminLayout` (rutas bajo `/admin/*`).
- `features/public/...` y `features/auth/...` cuelgan de `PublicLayout`.
- Cada pantalla vive en su propia carpeta con sus 4 archivos; no hay barrels (`index.ts`).
- Nombre de clase = PascalCase del nombre de carpeta, sin sufijo `Component`
  (`login/` → `Login`, `listado-programas/` → `ListadoProgramas`, `admin-layout/` → `AdminLayout`).
  Selector = `app-` + kebab-case de la carpeta.
- `shared/` guarda piezas genéricas: `Paginacion`, `FocoModal` (atrapa el foco en modales) y
  `excel/` (`leerExcel`, `descargarLibro`: toda importación/exportación de Excel debe usarlo en
  vez de importar `exceljs` directamente). Lo que es propio de una pantalla (p. ej. cómo se
  mapean las columnas a un DTO) queda en la carpeta de esa pantalla, como
  `importar-programas/mapear-filas.ts`.
- Los demás patrones visuales repetidos (tarjeta, tabla, buscador, badges) se **copian y
  adaptan** de la pantalla existente más parecida (hoy, la referencia es
  `features/admin/programas/listado-programas`). Si se repiten una tercera vez, valorar
  extraerlos a `shared/`.

## Conexión con el backend (`models/` + `services/`)

Los componentes **nunca** usan `HttpClient` directamente: inyectan un servicio de `services/`.
La estructura es un espejo del backend para encontrar cada pieza en ambos lados:

| Frontend | Backend |
|---|---|
| `services/programa.service.ts` → `ProgramaService` | `controller/ProgramaController` (`/api/programas`) |
| `services/universidad.service.ts` → `UniversidadService` | `controller/UniversidadController` (`/api/universidades`) |
| `models/programa.model.ts` → `Programa`, `ProgramaRequest` | `dto/response/ProgramaResponse`, `dto/request/ProgramaRequest` |
| `models/universidad.model.ts` → `Universidad` | `dto/response/CatalogoResponse` |
| `models/importacion-programas.model.ts` | `dto/*/ImportacionProgramas*` |
| `models/api-error.model.ts` → `ApiError` + `services/api-error.ts` | `exception/ApiExceptionHandler` |

Al conectar un módulo nuevo (p. ej. servicios, residentes):

1. Crear `models/<recurso>.model.ts` con una interfaz por DTO; comentar arriba de cada una el
   DTO del backend al que corresponde (`/** Backend: dto/response/XResponse. */`).
2. Crear `services/<recurso>.service.ts` (`@Injectable({ providedIn: 'root' })`), con URL
   `` `${API_URL}/<recurso>` `` y **los mismos nombres de método que el controller**
   (`listar`, `consultar`, `crear`, `actualizar`, `desactivar`…). Cada método devuelve el
   `Observable` de `HttpClient` y lleva un comentario con verbo y ruta (`/** GET /programas */`).
3. Añadir `<recurso>.service.spec.ts` con `provideHttpClientTesting()` comprobando verbo, URL y body.
4. En el componente: `inject(<Recurso>Service)`, `await firstValueFrom(...)` dentro de
   `try/catch` y mostrar `mensajeError(e)` en un signal de error.
5. En el spec del componente, sustituir el servicio con `{ provide: XService, useValue: mock }`.

`/api` lo redirige `proxy.conf.json` a `localhost:8080` en desarrollo (el backend usa
`context-path: /api`).

## Convenciones de componentes

- Standalone: `imports: [...]` en el decorador, sin módulos.
- Estado con signals (`signal`, `computed`), no `BehaviorSubject` ni servicios de estado.
- Control de flujo nativo de plantillas: `@for`/`@if`/`@empty` (no `*ngFor`/`*ngIf`).
- Los listados consultan el backend mediante services; no agregar constantes DEMO para sustituir datos reales. Si falta un endpoint, implementar la integración o mostrar un estado vacío/error explícito.
- Los miembros de la clase usados solo por la plantilla son `protected`.

## Diseño: colores y tipografía (`frontend/src/styles.css`)

Todo el diseño se apoya en variables CSS definidas en `:root`; **no usar valores hex sueltos
en los componentes**, siempre referenciar la variable.

| Variable | Valor | Uso |
|---|---|---|
| `--husi-navy` | `#0b2d5c` | Azul de marca, títulos, texto en tablas resaltado |
| `--husi-navy-deep` | `#071f42` | Variante oscura del navy |
| `--husi-blue` | `#14508f` | Hover de botones/enlaces navy |
| `--husi-deep` | `#02326f` | Cierre del degradado azul en pantallas públicas |
| `--husi-teal` | `#4ad3c8` | Acento de marca |
| `--husi-teal-dark` | `#2bb3a8` | Focus rings, badges de estado positivo |
| `--husi-bg` | `#ffffff` | Fondo base |
| `--husi-surface` | `#f6f9fd` | Fondo hover de filas de tabla |
| `--husi-border` | `#e5ecf5` | Bordes de tarjetas, inputs, separadores |
| `--husi-green` / `--husi-green-dark` | `#0e8a78` / `#0a6f60` | Botón de acción primaria del panel admin (Agregar, Exportar…) |
| `--husi-red` / `--husi-red-dark` | `#e01b1b` / `#b41414` | Acción destructiva / badge de estado negativo |
| `--husi-text` | `#1f2a3a` | Texto principal |
| `--husi-muted` | `#6b7a90` | Texto secundario, placeholders |
| `--font-serif` | Playfair Display | Títulos, encabezados de tabla, badges, marca |
| `--font-sans` (Inter) | — | Cuerpo de texto |
| `--radius-sm` / `--radius-md` / `--radius-lg` | `10px` / `16px` / `24px` | Border-radius estándar |
| `--shadow-sm` / `--shadow-md` / `--shadow-lg` | — | Sombras estándar |
| `--sidebar-width` | `224px` | Ancho fijo del sidebar admin |

Nota: el archivo de Figma del proyecto usa una paleta ligeramente distinta a la ya
implementada (p. ej. navy `#00457c` y teal `#019796` en vez de las variables de arriba).
Al traer una pantalla nueva de Figma, **priorizar la consistencia con las pantallas ya
construidas** (usar las variables de esta tabla) sobre el hex exacto del diseño, salvo que
el usuario pida fidelidad pixel-perfect.

## Layout admin (`layout/admin/admin-layout`)

- Sidebar fijo a la izquierda (`--sidebar-width`) con degradado navy y navegación por grupos
  (`navGroups`); el grupo "Administracion" agrupa Usuarios/Historial/Ajustes.
- Las pantallas de `/admin/*` solo maquetan su propio contenido (`<main class="admin-main">`
  ya lo entrega el layout); no repitas el sidebar en cada pantalla.
- Patrón de página admin establecido por `listado-programas`:
  - `.page-head` (título serif + breadcrumb + botón de acción primaria a la derecha)
  - `.card` (tarjeta blanca con borde `--husi-border` y `--shadow-sm`)
  - `.search` (buscador con icono, dentro de la tarjeta)
  - tabla con encabezado en `#e8f0f9`, filas alternadas por hover, y `.paginacion` centrada
    con botones `‹ 1 2 ›`.

## Rutas

Todo el árbol vive en `app.routes.ts` (un solo archivo, sin lazy loading por ahora).
Al agregar una pantalla admin: crear el componente en `features/admin/<módulo>/<pantalla>/`,
importarlo en `app.routes.ts` y añadir su `{ path, component }` como hijo de `AdminLayout`.
El link del sidebar (`admin-layout.ts`) normalmente ya existe apuntando a la ruta futura.

## Organización ampliada

Consultar docs/organizacion-codigo.md. Los formularios de programas, universidades y rotaciones viven en su propia carpeta del módulo. Consulta está en shared/consulta y sus tipos de paginación en models. Los estilos comunes de formularios están en shared/formularios.
