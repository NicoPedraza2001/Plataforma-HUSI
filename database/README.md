# Base de datos HUSI

PostgreSQL 18 en Docker. Todo el esquema y los datos iniciales se crean solos
la primera vez que se levanta el contenedor, para que la base sea idéntica en
todos los computadores del equipo.

## Levantar

La primera vez, crea el archivo `.env` (ignorado por git) con la contraseña del
administrador `admin@husi.com`:

```bash
copy .env.example .env      # luego edita .env y escribe HUSI_ADMIN_CONTRASENA_INICIAL
```

```bash
docker compose up -d --build
```

- `--build` reconstruye la imagen del backend con el código actual; sin él, Docker
  reutiliza la imagen anterior.
- Al arrancar, el backend guarda el hash BCrypt de esa contraseña. Solo pasa la primera
  vez: si el admin ya tiene contraseña, la variable se ignora (se cambia en Usuarios).
- El frontend se sigue levantando con `npm start` desde `frontend` (su proxy envía
  `/api` al backend del contenedor en `localhost:8080`).

- PostgreSQL: `localhost:5432` — base `husi`, usuario `husi`, contraseña `husi_dev`.
- pgAdmin: <http://localhost:5050> — `admin@husi.com` / `admin_dev`. El servidor
  **HUSI PostgreSQL** ya viene registrado (`database/pgadmin/servers.json`);
  al conectarse pide la contraseña del usuario `husi`.

## Cómo funciona la carga inicial

La carpeta `database/init/` se monta en `/docker-entrypoint-initdb.d` dentro del
contenedor. La imagen oficial de PostgreSQL ejecuta esos `.sql` **en orden
alfabético y una sola vez**: cuando el volumen `postgres_data` está vacío. Si un
script falla, el arranque se detiene y el error queda en `docker compose logs postgres`.

| Archivo | Contenido | Filas |
|---|---|---|
| `01_crear_tablas.sql` | Tablas, índices y las 4 vistas de control | — |
| `02_funciones.sql` | Funciones de cupo, auditoría y consulta | — |
| `03_triggers.sql` | Triggers del ciclo de vida del cupo | — |
| `04_datos_rol_usuario.sql` | Roles y usuario administrador | 2 roles, 1 usuario |
| `05_datos_universidad_programa.sql` | Universidad y programas | 1 + 21 |
| `06_datos_servicio.sql` | Servicios del hospital | 34 |
| `07_datos_asignatura.sql` | Asignaturas | 229 |
| `08_datos_residente.sql` | Residentes | 214 |
| `09_datos_rotacion.sql` | Rotaciones | 586 |
| `10_datos_rotacion_residente.sql` | Asignaciones residente–rotación | 712 |

El orden importa: las funciones necesitan las tablas, los triggers necesitan las
funciones, y los datos necesitan los triggers. `09` no envía `cupo_disponible`
ni `estado` (los llena `trg_inicializar_cupo_rotacion`) y `10` no envía `estado`
(lo decide `trg_rr_insert` según haya cupo o no).

## Reconstruir desde cero

Los scripts **no** se vuelven a ejecutar sobre un volumen que ya tiene datos.
Para partir de cero (borra todo lo que haya en la base):

```bash
docker compose down -v
```

```bash
docker compose up -d
```

## Verificar la carga

`database/verificacion.sql` no se ejecuta solo. Se corre a mano desde pgAdmin o
así:

```bash
docker compose exec -T postgres psql -U husi -d husi -f - < database/verificacion.sql
```

Comprueba los conteos por tabla, que las 712 asignaciones quedaron en
`Asignado` (nadie en lista de espera) y que el contador de cupo es consistente
con las filas asignadas.

## Pendientes conocidos

- **Contraseña del administrador**: `04_datos_rol_usuario.sql` trae un marcador
  de posición en lugar del hash. Reemplazarlo por uno real generado con
  `BCryptPasswordEncoder` antes de probar el login.
- **Cupos de los servicios**: `06_datos_servicio.sql` usa cupos provisionales.
  El Excel no trae `cupo_maximo_instalado` ni `cupo_simultaneo`, así que cada
  servicio arranca con 50 de ambos. Reemplazarlos cuando la Oficina de Educación
  entregue las capacidades reales.
- **4 filas duplicadas del Excel**: quedaron fuera de
  `10_datos_rotacion_residente.sql` porque violan la llave primaria. Están
  documentadas como comentario al inicio de ese archivo.



## Actualización de rotaciones (2026-09-14)

Para bases ya creadas, aplicar `migrations/20260914_rotacion_activa.sql` antes de iniciar el nuevo backend. Es una migración transaccional aditiva: agrega `rotacion.activa`, conserva la disponibilidad y protege nuevas asignaciones a rotaciones inactivas. No borrar volúmenes ni volver a cargar los datos. Las bases nuevas ejecutan `init/11_rotacion_activa.sql` automáticamente. Ver `../docs/rotaciones.md`.

## Cupo de rotaciones ligado al servicio (2026-10-03)

Para bases ya creadas, aplicar `migrations/20261003_recalcular_cupo_servicio.sql`. Crea `trg_servicio_recalcular_cupo`: al cambiar `servicio.cupo_simultaneo` se recalcula `cupo_disponible` de todas sus rotaciones y se rechaza bajarlo por debajo de los residentes `Asignado` de cualquiera de ellas. También resincroniza una vez las rotaciones existentes y avisa (NOTICE) de servicios cuyo cupo ya es menor que sus asignados. Las bases nuevas ejecutan `init/13_recalcular_cupo_servicio.sql` automáticamente.

Después, aplicar `migrations/20261003_lista_espera.sql` (en ese orden). Agrega `rotacion_residente.fecha_registro` y la promoción automática de la lista de espera por orden de llegada. A las filas existentes les pone la hora de la migración, así que entre ellas el orden lo decide `id_residente`. Al final promueve una vez a quienes ya esperaban en rotaciones con cupo libre e informa cuántos con un NOTICE. Las bases nuevas ejecutan `init/14_lista_espera.sql`.

## Historial conserva al responsable (2026-11-04)

Para bases ya creadas, aplicar `migrations/20261104_historial_conserva_responsable.sql` antes de iniciar el nuevo backend. Agrega `nombre_responsable` y `correo_responsable` a `historial_auditoria`, los llena con los datos actuales de cada usuario y hace opcional `id_usuario` (`ON DELETE SET NULL`), de modo que eliminar un usuario conserva sus movimientos.
