# Plataforma HUSI

Plataforma de gestión de residentes del Hospital Universitario San Ignacio.

## Stack

- **Frontend**: Angular 21 (standalone components, sin `NgModule`), en `/frontend`.
- **Backend**: Spring Boot (API REST). Aún no hay código en el repo — se documentará aquí
  cuando exista `/backend`.
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
  app.routes.ts               # Único árbol de rutas de la app
```

- `features/admin/...` cuelga de `AdminLayout` (rutas bajo `/admin/*`).
- `features/public/...` y `features/auth/...` cuelgan de `PublicLayout`.
- Cada pantalla vive en su propia carpeta con sus 4 archivos; no hay barrels (`index.ts`).
- Nombre de clase = PascalCase del nombre de carpeta, sin sufijo `Component`
  (`login/` → `Login`, `listado-programas/` → `ListadoProgramas`, `admin-layout/` → `AdminLayout`).
  Selector = `app-` + kebab-case de la carpeta.
- Todavía no hay una carpeta de componentes compartidos (`shared/`, `ui/`, etc.). Los patrones
  visuales repetidos (tarjeta, tabla, buscador, badges, paginación) se **copian y adaptan**
  de la pantalla existente más parecida (hoy, la referencia es
  `features/admin/programas/listado-programas`). Si se repiten una tercera vez, valorar
  extraerlos a un componente compartido.

## Convenciones de componentes

- Standalone: `imports: [...]` en el decorador, sin módulos.
- Estado con signals (`signal`, `computed`), no `BehaviorSubject` ni servicios de estado.
- Control de flujo nativo de plantillas: `@for`/`@if`/`@empty` (no `*ngFor`/`*ngIf`).
- Datos que vienen del backend: se mockean con una constante `..._DEMO` y un comentario
  `// TODO: reemplazar por los datos que entregue el backend` justo encima. Los handlers
  que deberían llamar a un servicio real quedan con `// TODO: conectar con ...`.
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
