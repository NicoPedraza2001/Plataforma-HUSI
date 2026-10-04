# Carga inicial de residentes y rotaciones

Ruta: Rotaciones → Importar Excel. Rama: Desarrollo_Santi.

1. Archivo: .xlsx hasta 5 MB y 5000 registros por hoja.
2. Columnas: seleccionar hoja y encabezados; relacionar columnas o indicar valores comunes.
3. Revisar y ordenar: corregir filas y cambiar su prioridad con las flechas. El número identifica la fila original.
4. Validar: el backend compara con la base de datos sin escribir y reserva cupos en una simulación.
5. Confirmar: revisar cantidades por tabla antes del guardado.
6. Resultado: descargar el reporte con datos y mensajes o volver al listado. Tras guardar, «Descargar registros no creados» genera `registros-no-creados.xlsx`. Incluye las filas con error u omitidas, y también las que se excluyeron a mano en los pasos del asistente. Lleva los mismos encabezados de la carga, más «Fila original», «Motivo» y «Detalle», para corregirlas e importarlas de nuevo.

Volver de paso conserva las correcciones. Cambiar columnas o valores comunes reconstruye las filas; modificar datos o prioridad invalida la revisión anterior.

## Datos y reglas

La carga resuelve Universidad → Programa → Residente y Servicio → Asignatura → Rotación → Rotación_Residente. Puede crear catálogos faltantes si está marcada la opción correspondiente. No crea usuarios, roles ni coordinadores.

Se requieren universidad, programa, nombre, tipo y número de documento, servicio, asignatura y fechas. País es obligatorio para universidades nuevas; cupos máximo y simultáneo son obligatorios para servicios nuevos. Perfil académico es opcional. No se inventan cupos ni tipos de documento.

Los existentes se reutilizan sin sobrescribirlos. Las contradicciones, inactivos, fechas inválidas y longitudes incorrectas producen errores. Los duplicados se omiten, incluidas relaciones canceladas. Las filas sin cupo se importan igual y quedan en «Esperando cupo» (estado `ESPERA` en la validación, con su posición en la lista), en el orden de prioridad de la carga. Así la rotación conserva su lista de espera y se promueve sola cuando se libera o se amplía un cupo. No hay reasignación automática a otros servicios. La capacidad sigue el modelo actual: por rotación y período, no como capacidad compartida entre períodos solapados.

Se aceptan fechas de Excel o texto AAAA-MM-DD. Documentos con ceros iniciales deben estar como texto. Las fórmulas deben reemplazarse por valores. La plantilla definitiva de 1756 filas con datos cabe en el límite; los campos ausentes deben completarse antes de confirmar.

## Organización y API

Frontend: importar-rotaciones coordina los pasos; revisar-carga edita y ordena; campos-carga define mapeo; models/carga-inicial.model.ts contiene contratos; services/carga-inicial.service.ts centraliza HTTP. shared/excel mantiene lectura y descarga.

Backend: CargaInicialController → CargaInicialService/Impl → CargaInicialRepository. CargaInicialPlan realiza la simulación. El guardado reutiliza los servicios CRUD existentes en una sola transacción.

POST /api/rotaciones/carga-inicial/validar
POST /api/rotaciones/carga-inicial/confirmar

La confirmación exige la huella de la revisión. Si cambia el contenido o la base consultada, devuelve 409 y requiere revalidar. Bloquea registros existentes durante el guardado; cualquier fallo revierte la transacción completa. Los endpoints anteriores de importación de rotaciones se conservan por compatibilidad.

## Verificación

72 pruebas backend aprobadas (PostgreSQL y H2), 80 frontend aprobadas; compilación Angular y Java correctas. Se prueban relaciones, duplicados, prioridad, falta de cupos, campos inválidos, cambios desde la revisión, navegación y errores HTTP. Permanecen advertencias anteriores de tamaño del bundle y ExcelJS CommonJS.

## Plantilla definitiva recibida

Archivo: Plantilla Rotaciones modificada- proyecto rotaciones.xlsx.
Encabezados oficiales: Categoría, Programa, Documento, Name, Perfil, Universidad, País, Rotaciones /asignaturas, id_sede, Servicio_HUSI, Fecha inicial, Fecha final.

Perfil se relaciona con perfil académico del residente. Categoría e id_sede no tienen equivalencia en las entidades actuales y son informativos; no se inventan tablas ni asociaciones. La plantilla descargable reproduce los doce encabezados oficiales.

El archivo recibido tiene 1756 filas con datos y formato hasta la fila 4343. Solo 8 filas contienen universidad y fechas; muchos servicios están vacíos. Se deben completar los faltantes; no es posible importar correctamente esos registros sin esa información. Las filas totalmente vacías se ignoran.

Un valor común aplicado a una columna solo completa celdas vacías. El tipo de documento se precarga como CC por confirmación del usuario y puede modificarse. Los cupos existentes se consultan; solo se solicitan para servicios nuevos.

Verificación tras adaptar la plantilla: 80 pruebas frontend aprobadas; 72 backend aprobadas.
El usuario confirmó que todos los documentos de la plantilla son CC. Se precarga CC como valor común editable, sin reemplazar un tipo informado en una columna.

Prueba final en navegador: carga ficticia confirmada en H2, una asignación creada con sus siete entidades/relaciones y una fila excluida por falta de cupo. Fue necesario reiniciar el backend local antes de completar el guardado. No se importaron los datos reales del archivo recibido.

