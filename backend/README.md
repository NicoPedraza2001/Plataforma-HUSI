# Backend - Sistema de Gestion de Residentes HUSI

API REST en Spring Boot que soporta la plataforma de gestion de residentes y
rotaciones del Hospital Universitario San Ignacio.

## Stack

| Componente     | Version | Nota                                              |
|----------------|---------|---------------------------------------------------|
| Java           | 21      | Version objetivo de compilacion (`java.version`)  |
| Spring Boot    | 4.0.8   | Web, Data JPA, Validation                         |
| PostgreSQL     | 18.6    | Contenedor definido en `docker-compose.yml`       |
| springdoc      | 3.0.3   | Swagger UI para probar los endpoints              |
| Lombok         | -       | Reduce el codigo repetitivo de entidades y DTOs   |

El esquema de base de datos **no lo gestiona el backend**: lo crean los scripts
de `database/init`, que Postgres ejecuta al levantar el contenedor por primera
vez. Por eso Hibernate corre con `ddl-auto: validate` y nunca modifica tablas.

## Como levantarlo

1. Base de datos arriba:

   ```
   docker compose up -d postgres
   ```

2. Backend (no hace falta instalar Maven, el wrapper lo resuelve). La primera
   vez, define la contrasena del administrador `admin@husi.com` (no se guarda
   en el repositorio; el backend guarda su hash BCrypt al arrancar):

   ```
   $env:HUSI_ADMIN_CONTRASENA_INICIAL = "<minimo 8 caracteres>"   # PowerShell
   ```


   ```
   cd backend
   ./mvnw spring-boot:run        # Linux / Mac / Git Bash
   mvnw.cmd spring-boot:run      # PowerShell / CMD
   ```

3. Comprobar que respondio:

   - API .......... http://localhost:8080/api
   - Swagger UI ... http://localhost:8080/api/swagger-ui.html
   - OpenAPI ...... http://localhost:8080/api/v3/api-docs

## Estructura por capas

Todo cuelga del paquete raiz `co.edu.javeriana.husi`. Una peticion recorre
siempre el mismo camino:

```
HTTP  ->  controller  ->  service (interfaz)  ->  service.impl  ->  repository  ->  PostgreSQL
              |                                       |
            dto/request                            mapper  ->  model/entity
            dto/response
```

| Paquete          | Responsabilidad                                                              |
|------------------|------------------------------------------------------------------------------|
| `config`         | CORS, OpenAPI y beans transversales.                                          |
| `controller`     | Endpoints REST. Reciben, validan el DTO y delegan. Sin logica de negocio.     |
| `service`        | Interfaces de negocio: el contrato que consume el controller.                 |
| `service/impl`   | Implementacion de las reglas (cupos, estados, validaciones de dominio).       |
| `repository`     | Interfaces `JpaRepository`, una por entidad.                                  |
| `model/entity`   | Entidades JPA, espejo de las tablas de `database/init/01_crear_tablas.sql`.   |
| `model/enums`    | Enums del dominio (estados de registro, rotacion y asignacion).               |
| `dto/request`    | Datos que entran del frontend, con anotaciones de validacion.                 |
| `dto/response`   | Datos que salen al frontend. Nunca se expone una entidad JPA.                 |
| `mapper`         | Conversion entidad <-> DTO.                                                   |
| `exception`      | Excepciones propias y el manejador global `@RestControllerAdvice`.            |
| `security`       | Spring Security: sesion en el servidor, roles, CSRF y BCrypt.                |
| `util`           | Utilidades sin estado.                                                        |

## Como agregar un caso de uso

Trabajando por casos de uso (por ejemplo el CRUD de Servicio), el orden que
evita retrabajo es:

1. `model/entity/Servicio.java` - mapear la tabla tal como existe en la BD.
2. `repository/ServicioRepository.java` - `extends JpaRepository<Servicio, Integer>`.
3. `dto/request/ServicioRequest.java` y `dto/response/ServicioResponse.java`.
4. `mapper/ServicioMapper.java` - conversion entre los dos mundos.
5. `service/ServicioService.java` + `service/impl/ServicioServiceImpl.java`.
6. `controller/ServicioController.java` - `@RequestMapping("/servicios")`.

Al terminar, el endpoint queda visible en Swagger UI para probarlo.

## Perfiles de configuracion

| Perfil | Archivo                 | Uso                                                        |
|--------|-------------------------|------------------------------------------------------------|
| `dev`  | `application-dev.yml`   | Local. Activo por defecto. Apunta al contenedor de Docker. |
| `prod` | `application-prod.yml`  | Despliegue. Credenciales por variables de entorno.         |

Para forzar un perfil: `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"`.

## Pruebas

Las pruebas por caso de uso están en `src/test/java/.../casosdeuso`. Su nombre visible empieza por
el flujo de `docs/pruebas/plan-pruebas-junit.md` (por ejemplo, `CU07-EX6`).

| Clases                 | Dónde corren                                                          |
|------------------------|-----------------------------------------------------------------------|
| `*IntegrationTests`    | PostgreSQL, con sus triggers reales. Cada prueba revierte sus datos.  |
| `*H2IntegrationTests`  | H2 con el perfil `local`. Comprueban que los triggers Java de `config/local` se comportan igual, y la seguridad por HTTP. |
| `*Test`                | Unitarias, sin Spring ni base de datos.                               |

```
docker compose up -d postgres
mvnw.cmd clean test                                # Toda la suite, unos 10 minutos
mvnw.cmd test "-Dtest=*H2IntegrationTests"         # Solo H2, sin Docker
mvnw.cmd test "-Dtest=*Test"                       # Solo unitarias, sin Docker
mvnw.cmd test "-Dtest=CU07*"                       # Un caso de uso
```

Los resultados quedan en `target/surefire-reports` y la cobertura de JaCoCo en
`target/site/jacoco/index.html`. No fuerces el perfil `local` ni `SPRING_DATASOURCE_URL` para toda
la suite: cada clase ya elige su base.

## Dependencias pendientes

En `pom.xml` quedan comentadas, listas para activarse cuando llegue su caso de uso:

- **Spring Security + JJWT** - login con roles Administrador y Coordinador.
- **Apache POI** - carga masiva desde archivos Excel.

Se dejaron desactivadas a proposito: agregar `spring-boot-starter-security`
antes de tiempo protege todos los endpoints por defecto y estorba mientras se
construyen los primeros CRUD.
