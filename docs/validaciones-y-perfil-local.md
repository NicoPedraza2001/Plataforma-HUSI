# Organización TypeScript, validaciones y ejecución local

## Organización del frontend
Se revisaron los archivos TypeScript de `frontend/src`. Las importaciones están ordenadas y sin importaciones sin uso. En las clases se agrupan dependencias, propiedades de estado, constructor, acciones y métodos privados. Se conserva el orden relativo de inicialización del estado.

Cada módulo conserva sus carpetas de listado y formulario, con TS, HTML, CSS y pruebas separados. Los componentes llaman a `services/`; no contienen HTTP directo. `models/` describe los contratos. No se agregó una clase base genérica que oculte el funcionamiento de los CRUD.

Las validaciones repetidas están en `shared/formularios/validadores.ts` y se conectan a los formularios mediante `restricciones-entrada.ts`. Rechazan textos obligatorios vacíos o con espacios, enteros fuera de rango y contraseñas incompatibles con BCrypt. El formulario de Rotaciones concentra en `puedeGuardar` la condición de fechas, selección, carga y edición permitida. Los envíos también validan el formulario, no solamente el botón.

## Restricciones: tres niveles
1. Frontend: respuesta inmediata; campos obligatorios, `maxlength`, correo, enteros, fechas y validadores compartidos.
2. Backend: DTO con Bean Validation y JSON estricto; servicios comprueban duplicados, referencias, estado de padres y reglas entre campos. La base de datos sigue siendo la autoridad si hay dos peticiones concurrentes.
3. Base de datos: PK, FK, NOT NULL, UNIQUE, longitudes y CHECK. Las entidades JPA declaran los metadatos correspondientes; Hibernate valida el esquema y no modifica PostgreSQL.

| Entidad | Límites y restricciones principales |
| --- | --- |
| Universidad | Nombre 150, país 100 obligatorios; perfil 50 opcional; nombre único |
| Programa | Nombre 150 y universidad obligatorios; combinación universidad/nombre única |
| Residente | Nombre 200, tipo documento 30, documento 50 obligatorios; documento único; perfil académico 100 opcional; programa existente |
| Usuario | Nombre y apellido 100, correo 150 y rol obligatorios; correo válido y único; contraseña nueva de al menos 8 caracteres y máximo 72 bytes UTF-8; hash almacenado hasta 255 |
| Servicio | Nombre 150 único; descripción opcional sin límite artificial (TEXT); cupos enteros 0–2147483647; simultáneo no supera máximo |
| Asignatura | Nombre 200 y servicio obligatorios; combinación servicio/nombre única |
| Coordinador | Usuario y servicio obligatorios; usuario único, activo, con rol Coordinador al asignarlo |
| Rotación | Asignatura y fechas obligatorias; fin igual o posterior al inicio; fechas 1900–9999; clave compuesta única; cupos calculados, nunca editados desde el formulario |
| Asignación | Residente y rotación existentes; clave compuesta única; Asignado, Esperando cupo o Cancelado |

No se restringe el documento a números: la BD admite texto y los pasaportes pueden ser alfanuméricos. Tampoco se inventó un catálogo cerrado de tipos de documento que el modelo no tiene. Los estados administrativos editables son Activo/Inactivo; Eliminado existe en el esquema histórico, pero no se ofrece como estado editable.

Los errores de formato y validación responden 400; duplicados y referencias que impiden eliminar responden 409. Los mensajes de validación están en español. El frontend nunca decide la unicidad a partir de la página cargada.

## Perfiles de Spring
El proyecto utiliza YAML, equivalente a `application.properties`; se conserva esa convención:

- `application.yml`: configuración común y validación estricta de JSON.
- `application-dev.yml`: PostgreSQL local, perfil predeterminado existente.
- `application-local.yml`: H2 dentro del proceso Java, sin Docker.
- `application-prod.yml`: PostgreSQL con variables de entorno.

### Ejecutar sin Docker (PowerShell)
En `backend`:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=local" spring-boot:run
```

En otra terminal, en `frontend`:

```powershell
npm start
```

Abrir `http://localhost:4200/admin/universidades`. La API sigue en `http://localhost:8080/api`; no hay que cambiar los servicios Angular ni el proxy. Solo un backend puede ocupar 8080: detener el anterior antes de iniciar otro perfil. Se necesita Java 21 y Node; Maven Wrapper descarga las dependencias la primera vez.

**H2 es una base de demostración en memoria:** al detener el proceso Java se pierden los datos de esa sesión. Al volver a iniciarlo se cargan los ejemplos ficticios de `db/local/data.sql`. Los datos de PostgreSQL no se copian, borran ni modifican. No activar `local` junto con `dev` o `prod` ni reutilizar el esquema local contra una base real.

`db/local/schema.sql` conserva las restricciones de las tablas. Los disparadores Java de `config/local/` reproducen inicialización, consumo, espera, cancelación y protección de cupos. Se registran únicamente en el esquema H2; PostgreSQL sigue usando sus funciones SQL existentes. Las vistas y funciones auxiliares PostgreSQL no usadas por la API no se portan como una consola de administración H2. El perfil no habilita una consola web de base de datos.

### Volver a PostgreSQL
Detener el backend local y ejecutar en `backend`:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
```

### Verificación
Solo H2, sin Docker:

```powershell
.\mvnw.cmd "-Dtest=LocalH2IntegrationTests" test
```

Toda la suite backend (incluye las pruebas PostgreSQL existentes; requiere PostgreSQL): ` .\mvnw.cmd test`.
Frontend: `npm test -- --watch=false`, `npm run build` y `npm run format:check`.

Las pruebas H2 recorren todos los listados, búsqueda, filtros, creación de entidades relacionadas, cambios de estado, borrados permitidos/protegidos, asignación/espera/cancelación, tipos incorrectos, nulos, longitudes, duplicados y restricciones SQL directas.
