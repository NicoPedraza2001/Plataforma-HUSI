# Base de datos HUSI

PostgreSQL 18 en Docker. Todo el esquema y los datos iniciales se crean solos
la primera vez que se levanta el contenedor, para que la base sea idéntica en
todos los computadores del equipo.

## Levantar

```bash
docker compose up -d
```

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
  El Excel no trae `cupo_maximo_instalado` ni `cupo_simultaneo`, así que se puso
  el mínimo que permite cargar los datos sin lista de espera. Reemplazarlos
  cuando la Oficina de Educación entregue las capacidades reales.
- **4 filas duplicadas del Excel**: quedaron fuera de
  `10_datos_rotacion_residente.sql` porque violan la llave primaria. Están
  documentadas como comentario al inicio de ese archivo.


