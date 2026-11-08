# Plan de pruebas JUnit del backend

Borrador para aprobar antes de escribir las pruebas. Regla adoptada: **al menos una prueba por cada
flujo del caso de uso: el principal y cada flujo alterno o excepción.**

## 1. De dónde salen los flujos

- **Casos de uso:** los 12 códigos del SRS, sección 3.2 (CU-01 a CU-12).
- **Flujos:** ni el SRS ni el SDD los escriben como principal / alterno / excepción. El SDD solo
  trae la tabla 4 y el detalle de la asignación (3.2.1). Aquí se **derivaron** de los criterios de
  medición de cada RF, de las reglas de negocio (SRS 6.2) y de las excepciones reales del código.
- **Nomenclatura:** `FP` flujo principal, `FA` flujo alterno, `EX` excepción.
- **Estado:** ✅ cubierto, 🟡 parcial, ❌ falta. Se asignó por el
  nombre de las pruebas y la lectura parcial de su contenido. Cada fila se confirma al implementarla.

## 2. Situación actual

- La suite del backend tiene **678 ejecuciones de prueba** y todas pasan con `mvnw clean test`. Las reglas de negocio corren en PostgreSQL y también en H2; la seguridad corre por HTTP sobre H2.
- Las pruebas por caso de uso están en `backend/src/test/java/.../casosdeuso` y su `@DisplayName` empieza por el identificador del flujo de esta matriz.
- JaCoCo deja el reporte de cobertura en `backend/target/site/jacoco/index.html`.
- **CU01-FA2** comprueba mayúsculas: la validación de la API rechaza un correo con espacios alrededor (400), así que el navegador debe enviarlo sin ellos.
- **CU01-EX8** comprueba la duración configurada de 30 minutos y el rechazo de una cookie desconocida; no espera los 30 minutos reales.

**Resumen de la matriz:** 186 flujos, 186 cubiertos.

| Caso de uso | Flujos | Cubiertos | Pendientes |
|---|---:|---:|---:|
| CU-01 | 18 | 18 | 0 |
| CU-02 | 12 | 12 | 0 |
| CU-03 | 15 | 15 | 0 |
| CU-04 | 28 | 28 | 0 |
| CU-05 | 13 | 13 | 0 |
| CU-06 | 14 | 14 | 0 |
| CU-07 | 19 | 19 | 0 |
| CU-08 | 16 | 16 | 0 |
| CU-09 | 16 | 16 | 0 |
| CU-10 | 13 | 13 | 0 |
| CU-11 | 8 | 8 | 0 |
| CU-12 | 9 | 9 | 0 |
| Transversales | 5 | 5 | 0 |

## 3. Decisiones

| # | Tema | Decisión aplicada |
|---|---|---|
| D1 | Eliminar un usuario (RF-04) | Se rechaza si coordina un servicio o si es la cuenta en sesión. Sus movimientos no lo impiden y se conservan: cada fila guarda el nombre y el correo del responsable. **Falta ajustar el texto del RF-04 en el SRS**, que todavía prohíbe eliminar a quien tenga movimientos |
| D2 | Cambiar correo o contraseña (RF-06) | La sesión se cierra y el frontend lleva al login con un aviso |
| D3 | Sesión abierta tras cambios en la cuenta (RF-03, RNF-08) | `SesionVigenteFiltro` termina la sesión con 401 si la cuenta se eliminó, se desactivó o cambió de rol, correo o contraseña |
| D4 | Base de datos de las pruebas | Reglas de negocio contra PostgreSQL; cupos y lista de espera también contra H2 |
| D5 | Último administrador activo | **Pendiente.** Se puede desactivar. Eliminarlo no es posible, porque nadie puede eliminar su propia cuenta. Preguntar al equipo; no se prueba hasta definirlo |

## 4. Matriz de flujos por caso de uso

### CU-01 Iniciar y cerrar sesión (RF-01, RF-02, RF-03, RNF-07, RNF-08, RNF-10)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU01-FP1 | Administrador ingresa: 200, perfil con su rol y acceso a su panel | ✅ | `elAdministradorIniciaSesionYEntraAlPanel` |
| CU01-FP2 | Coordinador ingresa: 200, perfil con su rol y acceso a su panel | ✅ | `CU01SesionH2IntegrationTests` |
| CU01-FP3 | Cerrar sesión invalida la sesión: la siguiente petición da 401 | ✅ | `cerrarSesionInvalidaLaSesion` |
| CU01-FA1 | Un ingreso correcto reinicia el contador: 4 fallos, éxito, 4 fallos no bloquean | ✅ | `LimiteIntentosInicioSesionTest` |
| CU01-FA2 | El correo se acepta sin distinguir mayúsculas | ✅ | `CU01SesionH2IntegrationTests`, `LimiteIntentosInicioSesionTest` |
| CU01-FA3 | Al vencer el bloqueo se recuperan todos los intentos (unitaria) | ✅ | `LimiteIntentosInicioSesionTest` |
| CU01-EX1 | Contraseña incorrecta o correo inexistente: 401 con el mismo mensaje | ✅ | `credencialesIncorrectasDanElMismoMensaje...` |
| CU01-EX2 | Usuario inactivo no ingresa, con el mismo mensaje | ✅ | `CU01SesionH2IntegrationTests` |
| CU01-EX3 | Cinco fallos bloquean y ni la contraseña correcta entra: 429 | ✅ | `bloqueaElCorreoTrasVariosIntentosFallidos` |
| CU01-EX4 | El bloqueo de un correo no afecta a otro | ✅ | `LimiteIntentosInicioSesionTest` |
| CU01-EX5 | Cuerpo inválido (correo o contraseña vacíos): 400 | ✅ | `CU01SesionH2IntegrationTests` |
| CU01-EX6 | Sin sesión: 401 y no se abre sesión en el servidor | ✅ | `sinSesionLaApiResponde401`, `unaPeticionSinSesion...` |
| CU01-EX7 | Sin token CSRF no se modifican datos: 403 | ✅ | `sinTokenCsrfNoSeModificanDatos` |
| CU01-EX8 | Sesión vencida o cookie desconocida: 401; expiración configurada en 30 min | ✅ | `CU01SesionH2IntegrationTests` |
| CU01-EX9 | Cookie de sesión `HttpOnly` y `SameSite=Strict`; el id cambia al ingresar (fijación) | ✅ | `CU01SesionH2IntegrationTests` |
| CU01-EX10 | Usuario desactivado o eliminado con sesión abierta: 401 | ✅ | `desactivarUnaCuentaTerminaSuSesionAbierta`, `eliminarUnaCuentaTerminaSuSesion...` |
| CU01-EX11 | Usuario que pierde el rol Administrador con sesión abierta: 401; al volver a ingresar, 403 en lo de administrador | ✅ | `quitarElRolDeAdministradorTerminaSuSesion...` |
| CU01-EX12 | Contraseña guardada como BCrypt, nunca en texto plano | ✅ | `laContrasenaSeGuardaComoHashBcrypt` |

La matriz completa de permisos por rol está en la sección 5 (T-01 y T-02).

### CU-02 Modificar los datos de la cuenta (RF-06)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU02-FP1 | Cambia nombres o apellidos sin pedir contraseña | ✅ | `ajustesActualizaSoloLosNombresEnviados` |
| CU02-FP2 | Cambia correo y contraseña con la contraseña actual | ✅ | `ajustesCambiaCorreoYContrasena...` |
| CU02-FA1 | Cambia solo el correo o solo la contraseña | ✅ | `CU02CuentaH2IntegrationTests` |
| CU02-EX1 | Contraseña actual incorrecta: 400 y no guarda | ✅ | `ajustesCambiaCorreoYContrasena...` |
| CU02-EX2 | Fallos repetidos de la contraseña actual suman al bloqueo: 429 | ✅ | `CU02CuentaH2IntegrationTests` |
| CU02-EX3 | Correo ya usado por otra cuenta: 409 | ✅ | `CU02CuentaH2IntegrationTests` |
| CU02-EX4 | Correo vacío o inválido: 400 | ✅ | `CU02CuentaH2IntegrationTests` |
| CU02-EX5 | Contraseña nueva de menos de 8 caracteres o más de 72 bytes: 400 | ✅ | `CU02CuentaH2IntegrationTests` |
| CU02-EX6 | Contraseña nueva igual a la actual: 400 | ✅ | `CU02CuentaH2IntegrationTests` |
| CU02-EX7 | Petición sin datos, o nombres solo con espacios: 400 | ✅ | `CU02CuentaH2IntegrationTests` |
| CU02-EX8 | Tras cambiar correo o contraseña hay que ingresar de nuevo, también en otras sesiones | ✅ | `ajustesCambiaCorreoYContrasena...`, `cambiarLasCredencialesCierraTambien...` |
| CU02-EX9 | El historial deja «Se cambió» y nunca la contraseña | ✅ | `CU02CuentaH2IntegrationTests` |

### CU-03 Gestionar usuarios y coordinadores (RF-04, RF-05)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU03-FP1 | Crea, edita sin cambiar la contraseña y guarda el hash | ✅ | `usuarioPersisteHashEditaSinCambiarClaveYElimina` |
| CU03-FP2 | Crea un coordinador con su servicio | ✅ | `elCoordinadorSeCreaDesdeUsuariosConSuServicio` |
| CU03-FP3 | Desactiva y reactiva; el vínculo de coordinador copia el estado | ✅ | `CU03UsuariosIntegrationTests` |
| CU03-FP4 | Elimina un usuario sin dependencias | ✅ | `usuarioPersisteHash...` |
| CU03-FA1 | Al cambiar de servicio, su panel presenta el nuevo | ✅ | `CU03UsuariosIntegrationTests` |
| CU03-FA2 | Al dejar de ser coordinador se retira su servicio | ✅ | `alDejarDeSerCoordinadorSeRetiraSuServicio` |
| CU03-FA3 | Varios coordinadores comparten servicio | ✅ | `variosCoordinadoresPuedenCompartirServicio` |
| CU03-FA4 | Conserva al editar el servicio inactivo que ya coordinaba | ✅ | `conservaElServicioInactivoQueYaCoordinaba` |
| CU03-FA5 | El administrador no guarda servicio | ✅ | `elAdministradorNoGuardaServicio` |
| CU03-EX1 | Correo duplicado, contraseña corta o rol inválido | ✅ | `usuarioRechazaCorreoDuplicadoClaveCortaYRolInvalido` |
| CU03-EX2 | Coordinador sin servicio, con servicio inexistente o inactivo | ✅ | `elCoordinadorExigeUnServicioActivo`, `rechazaServicioInactivo` |
| CU03-EX3 | Estado inválido: 400; usuario inexistente: 404 | ✅ | `CU03UsuariosIntegrationTests` |
| CU03-EX4 | Usuario con movimientos se elimina y sus movimientos se conservan | ✅ | `eliminarUsuarioConMovimientosConservaSuHistorial` |
| CU03-EX5 | Usuario que coordina un servicio no se elimina: 409; sin esa asignación, sí. Nadie elimina su propia cuenta | ✅ | `noSeEliminaAUnUsuarioQueCoordina...`, `alRetirarleElServicio...`, `unUsuarioNoPuedeEliminarSuPropiaCuenta` |
| CU03-EX6 | La respuesta nunca incluye la contraseña ni su hash | ✅ | `CU03UsuariosIntegrationTests` |

La contraseña sugerida (RF-04) la genera el frontend: se prueba en Vitest.

### CU-04 Gestionar los datos base (RF-07 a RF-12)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| **Universidades** | | | |
| CU04-U-FP1 | Crea, edita y consulta | ✅ | `crearEditarConsultarPersisteCampos...` |
| CU04-U-FP2 | Desactiva sin borrar programas ni residentes | ✅ | `desactivarNoBorraNiCambia...` |
| CU04-U-FP3 | Elimina si no tiene programas | ✅ | `eliminaFisicamenteProgramaSinResidentes...` |
| CU04-U-EX1 | Campos vacíos, nombre duplicado incluso inactivo, estado desconocido | ✅ | `rechazaCamposInvalidosDuplicadosYEstados...` |
| CU04-U-EX2 | No se elimina con programas, incluso inactivos | ✅ | `protegeProgramasInactivosAsociados...` |
| CU04-U-EX3 | Universidad inexistente o inválida: error explícito | ✅ | `CU04DatosBaseIntegrationTests` |
| **Programas** | | | |
| CU04-P-FP1 | Crea, consulta, actualiza y desactiva conservando residentes | ✅ | `crearConsultarActualizarYDesactivar...` |
| CU04-P-FA1 | Universidad externa solo admite su tipo de convenio | ✅ | `universidadExterna...`, `universidadNacional...`, `universidadInternacional...` |
| CU04-P-FA2 | Carga por Excel: validar no guarda e identifica cada fila | ✅ | `validacionNoGuardaYCargaMixta...` |
| CU04-P-EX1 | Duplicado en la universidad, incluso inactivo | ✅ | `rechazaDuplicadosInclusoInactivos...` |
| CU04-P-EX2 | Universidad inactiva no acepta programas ni permite reactivarlos | ✅ | `universidadInactivaNoAcepta...`, `noReactivaProgramaSiUniversidad...` |
| CU04-P-EX3 | No se elimina con residentes | ✅ | `protegeResidentesInactivosYNoFuerzaCascadas` |
| CU04-P-EX4 | El catálogo solo ofrece programas activos de universidades activas | ✅ | `CU04DatosBaseIntegrationTests` |
| CU04-P-EX5 | Carga por Excel: filas repetidas y universidad ambigua o desconocida | ✅ | `CU04DatosBaseIntegrationTests` |
| **Servicios y cupos** | | | |
| CU04-S-FP1 | Crea, edita, busca, cambia estado y elimina | ✅ | `crearEditarBuscarCambiarEstadoYEliminar` |
| CU04-S-FP2 | Aumentar el cupo recalcula las rotaciones y promueve la espera | ✅ | `cambiarCupoRecalcula...`, `ampliarCupoDelServicioPromueveLaEspera` |
| CU04-S-FA1 | Bajar el cupo justo hasta el número de asignados se permite (borde) | ✅ | `CU04DatosBaseIntegrationTests` |
| CU04-S-FA2 | Cambiar el cupo no toca rotaciones finalizadas | ✅ | `cambiarCupoDelServicioNoTocaRotacionesFinalizadas` |
| CU04-S-EX1 | Nombre duplicado; simultáneo mayor que el máximo | ✅ | `validaNombreDuplicadoYCapacidades` |
| CU04-S-EX2 | Bajar por debajo de los asignados, en el servicio y en la base | ✅ | `noPermiteBajarCupo...`, `laBaseTambienBloquea...` |
| CU04-S-EX3 | Cupos negativos o decimales | ✅ | `CU04DatosBaseIntegrationTests` |
| CU04-S-EX4 | No se elimina con asignaturas o coordinadores | ✅ | `asignaturasInclusoInactivasBloqueanBorrado`, `coordinadoresBloqueanBorrado` |
| **Asignaturas** | | | |
| CU04-A-FP1 | Crea, edita, busca, pagina, cambia estado y elimina | ✅ | `asignaturaPersisteEditaBuscaPagina...` |
| CU04-A-FA1 | El mismo nombre se admite en otro servicio | ✅ | `asignaturaPuedeRepetirseEnOtroServicio` |
| CU04-A-FA2 | Con rotaciones se puede renombrar y cambiar de estado | ✅ | `CU04DatosBaseIntegrationTests` |
| CU04-A-EX1 | Duplicada en el servicio; servicio inactivo | ✅ | `asignaturaDuplicadaSeRechaza...`, `rechazaServicioInactivo` |
| CU04-A-EX2 | Con rotaciones no cambia de servicio ni se elimina | ✅ | `rotacionesBloqueanCambioDeServicio`, `rotacionesProtegenAsignatura` |
| **Común (RF-12)** | | | |
| CU04-C-EX1 | Registros inactivos no aparecen en ningún catálogo de formulario | ✅ | `CU04DatosBaseIntegrationTests` |

### CU-05 Gestionar residentes (RF-13 a RF-16)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU05-FP1 | Crea, edita, filtra, cambia estado y elimina | ✅ | `residentePersisteEditaFiltra...` |
| CU05-FP2 | Listado paginado con programa y universidad | ✅ | `buscaEnTodaLaBaseYPaginaSinDuplicar` |
| CU05-FA1 | Se guarda con o sin perfil académico (RBD-09) | ✅ | `CU05ResidentesIntegrationTests` |
| CU05-FA2 | Pasaporte con letras se acepta | ✅ | `CU05ResidentesIntegrationTests` |
| CU05-FA3 | Búsqueda por documento sin distinguir mayúsculas | ✅ | `buscaResidentesPorDocumentos...` |
| CU05-FA4 | Desactivado conserva sus rotaciones | ✅ | `CU05ResidentesIntegrationTests` |
| CU05-EX1 | Documento duplicado; programa inválido | ✅ | `residenteRechazaDuplicadosYProgramaInvalido` |
| CU05-EX2 | Programa o universidad inactivos: 400 | ✅ | CU04DatosBaseIntegrationTests: `soloSeOfrecenProgramasActivos...` |
| CU05-EX3 | Tipo de documento o perfil académico inexistentes: 400 | ✅ | `CU05ResidentesIntegrationTests` |
| CU05-EX4 | Campos vacíos, solo espacios o más largos de lo permitido: 400 con el campo | ✅ | `CU05ResidentesIntegrationTests` |
| CU05-EX5 | Con rotaciones no se elimina: 409 | ✅ | `residenteAsociadoNoSeBorra` |
| CU05-EX6 | Estado inválido: 400; residente inexistente: 404 | ✅ | `CU05ResidentesIntegrationTests` |
| CU05-EX7 | Residente inactivo no aparece entre los candidatos de una rotación | ✅ | `CU05ResidentesIntegrationTests` |

### CU-06 Gestionar rotaciones (RF-21 a RF-25)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU06-FP1 | Crea, edita, cambia estado y elimina sin perder cupo | ✅ | `crearEditarEstadoYEliminarPersistenSinPerderCupo` |
| CU06-FP2 | Busca, filtra, pagina y cuenta residentes | ✅ | `busquedaFiltrosYPaginacionConsultanBase` |
| CU06-FP3 | Cupos libres = capacidad − asignados, y estado con cupo o llena | ✅ | `CU06RotacionesIntegrationTests` |
| CU06-FA1 | Filtro por rango devuelve las rotaciones que se cruzan | ✅ | `filtraPorRangoDeFechas...` |
| CU06-FA2 | Inicio igual a hoy se permite (borde) | ✅ | `CU06RotacionesIntegrationTests` |
| CU06-EX1 | Fin anterior al inicio; duplicada; asignatura inválida | ✅ | `validaFechasDuplicadosYAsignatura` |
| CU06-EX2 | Inicio anterior a hoy no se crea ni se mueve | ✅ | `noSeCreanNiMuevenRotaciones...` |
| CU06-EX3 | Asignatura o servicio inactivos: 400 | ✅ | `CU06RotacionesIntegrationTests` |
| CU06-EX4 | Fechas fuera de 1900–9999: 400 | ✅ | `CU06RotacionesIntegrationTests` |
| CU06-EX5 | Cambiar la asignatura: 400 | ✅ | `CU06RotacionesIntegrationTests` |
| CU06-EX6 | Con residentes, incluso cancelados, no cambia fechas ni se elimina | ✅ | `protegeRelacionesInclusoCanceladas` |
| CU06-EX7 | Rotación finalizada es de solo lectura | ✅ | `rotacionFinalizadaEsDeSoloLectura` |
| CU06-EX8 | Filtros desconocidos, fecha mal formada, «hasta» antes de «desde»: 400 | ✅ | `CU06RotacionesIntegrationTests` |
| CU06-EX9 | Rotación inexistente: 404 | ✅ | `CU06RotacionesIntegrationTests` |

### CU-07 Asignar residentes a una rotación (RF-26, RF-27)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU07-FP1 | Con cupo: Asignado | ✅ | `asignaEsperaCancelaYReasigna...` |
| CU07-FP2 | Sin cupo: Esperando cupo, al final de la lista | ✅ | `asignaEsperaCancelaYReasigna...` |
| CU07-FA1 | Rotación en curso admite residentes hasta su fecha final | ✅ | `rotacionEnCursoAdmiteResidentesHastaSuFechaFinal` |
| CU07-FA2 | Reasignar a un cancelado si hay cupo | ✅ | `asignaEsperaCancelaYReasigna...` |
| CU07-FA3 | Quitar el cupo: pasa al final y el cupo lo toma el primero en espera | ✅ | `quitarCupoMandaAlFinalDeLaEspera...` |
| CU07-FA4 | Quitar el cupo sin nadie en espera deja el cupo libre | ✅ | `quitarCupoSinNadieEnEspera...` |
| CU07-FA5 | Cancelar conserva la relación y no reactiva la rotación | ✅ | `cancelarAsignacionNoReactivaRotacion` |
| CU07-FA6 | Eliminar relación en espera o cancelada, también en rotación inactiva | ✅ | `CU07AsignacionIntegrationTests` |
| CU07-EX1 | Residente ya relacionado, incluso cancelado: 409 | ✅ | `asignacionRechazaDuplicado` |
| CU07-EX2 | Rotación inactiva no admite asignar, sí cancelar | ✅ | `inactivaPermiteCancelarPeroNoAsignar`, `inactivaBloquea...` |
| CU07-EX3 | Rotación finalizada: 409 | ✅ | `rotacionFinalizadaEsDeSoloLectura` |
| CU07-EX4 | Residente inactivo: 409; residente inexistente: 400 | ✅ | `CU07AsignacionIntegrationTests` |
| CU07-EX5 | Asignatura o servicio inactivos: 409 | ✅ | `CU07AsignacionIntegrationTests` |
| CU07-EX6 | Asignar manualmente sin cupo: 409 y permanece en su estado | ✅ | `CU07AsignacionIntegrationTests` |
| CU07-EX7 | Quitar el cupo a quien no está asignado: 409 | ✅ | `CU07AsignacionIntegrationTests` |
| CU07-EX8 | Estado inválido: 400; asignación inexistente: 404 | ✅ | `CU07AsignacionIntegrationTests` |
| CU07-EX9 | Eliminar a un asignado: 409 | ✅ | `soloSeEliminaUnaRelacionSinCupo` |
| CU07-EX10 | **Concurrencia:** dos asignaciones por el último cupo dejan una en espera | ✅ | `CU07ConcurrenciaIntegrationTests` |
| CU07-EX11 | Cada acción deja un movimiento; una acción rechazada no deja ninguno | ✅ | `CU07AsignacionIntegrationTests` |

### CU-08 Atender la lista de espera (RF-28, RF-29)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU08-FP1 | Cupo liberado va al primero en orden de llegada | ✅ | `cancelarAsignadoPromueveAlPrimeroEnEspera` |
| CU08-FP2 | La lista se consulta aparte y ordenada | ✅ | `listaLaEsperaAparteYPorOrdenDeLlegada` |
| CU08-FA1 | Aumentar el cupo promueve a varios, en orden | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-FA2 | Reactivar la rotación promueve | ✅ | `rotacionInactivaNoPromueveHastaReactivarse` |
| CU08-FA3 | Traslado a una rotación nueva del servicio: Asignado y sale de la anterior | ✅ | `trasladaEsperaANuevaRotacionDelMismoServicio` |
| CU08-FA4 | La lista de traslado marca los cruces de fechas | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX1 | **Residente inactivo se salta** y el cupo va al siguiente elegible | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX2 | Rotación inactiva no promueve | ✅ | `rotacionInactivaNoPromueve...` |
| CU08-EX3 | Rotación finalizada no promueve | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX4 | Asignatura o servicio inactivos no promueven | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX5 | Traslado superior a los cupos del destino: 409 | ✅ | `trasladoNoSuperaLosCuposDelDestino` |
| CU08-EX6 | Traslado con residente repetido: 400 | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX7 | Traslado desde otro servicio o de quien ya no espera: 409 y no cambia nada | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX8 | Traslado de residente ya en el destino o inactivo: 409 | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX9 | Destino finalizado o inactivo: 409 | ✅ | `CU08ListaEsperaIntegrationTests` |
| CU08-EX10 | **Paridad:** H2 y PostgreSQL promueven igual en todos los casos anteriores | ✅ | `*H2IntegrationTests`: las mismas pruebas de cupos y espera sobre H2 |

### CU-09 Importar desde Excel (RF-17 a RF-20): parte del servidor

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU09-FP1 | Validar no escribe; confirmar crea la cadena sin duplicar | ✅ | `validaSinEscribirYCreaTodaLaCadenaSinDuplicar` |
| CU09-FP2 | Respeta prioridad y deja en espera las filas sin cupo | ✅ | `respetaPrioridadYDejaEnEspera...` |
| CU09-FP3 | La importación queda como un solo movimiento con resumen y lote | ✅ | `unaImportacionQuedaComoUnSoloMovimiento...` |
| CU09-FA1 | Admite periodos históricos, a diferencia de la creación manual | ✅ | `CU09ImportacionIntegrationTests` |
| CU09-FA2 | Filas ya existentes se omiten sin modificarlas | ✅ | `importacionValidaSinGuardarOmiteDuplicados...` |
| CU09-FA3 | Cancelar conserva lo creado y no afecta las confirmadas | ✅ | `cancelarUnaImportacionConservaLoCreado...` |
| CU09-EX1 | Cero filas o más de 5000: 400 | ✅ | `CU09ImportacionIntegrationTests` |
| CU09-EX2 | Huella vacía o datos cambiados: 409 y no guarda | ✅ | `exigeRevalidarCuandoCambiaLaBase` |
| CU09-EX3 | Ninguna fila válida al confirmar: 400 | ✅ | `CU09ImportacionIntegrationTests` |
| CU09-EX4 | Campos inválidos no escriben | ✅ | `rechazaCamposInvalidosSinEscribir` |
| CU09-EX5 | Reglas de convenio y universidades externas | ✅ | `rechazaProgramasPropios...`, `rechazaConvenio...` (2) |
| CU09-EX6 | Un documento con identidades contradictorias | ✅ | `detectaIdentidadesContradictorias` |
| CU09-EX7 | Fechas, cupos y longitudes | ✅ | `fechasCuposYLongitudesSeValidanEnBackend` |
| CU09-EX8 | **Atomicidad:** si falla la operación final no queda ninguna rotación ni asignación | ✅ | `CU09AtomicidadIntegrationTests` |
| CU09-EX9 | Identificador de lote inválido: 400 | ✅ | `CU09ImportacionIntegrationTests` |
| CU09-EX10 | Importación de rotaciones: 0 o más de 1000 filas, números repetidos o negativos | ✅ | `CU09ImportacionIntegrationTests` |

### CU-10 Consultar el tablero (RF-30 a RF-32)

Es la **mayor brecha**: 317 líneas de servicio y sus consultas sin aserciones sobre las cifras.

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU10-FP1 | Sin filtros, los indicadores coinciden con datos preparados | ✅ | `CU10TableroIntegrationTests` |
| CU10-FP2 | Por periodo semestral, solo cuenta lo de ese periodo | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA1 | Cada filtro (universidad, programa, servicio, asignatura) recorta los resultados | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA2 | Las relaciones encadenan filtros: un servicio solo muestra sus asignaturas | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA3 | Base o periodo sin datos: panel vacío sin error ni división por cero | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA4 | Alerta de residentes con fechas cruzadas | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA5 | Alerta de residentes en lista de espera | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA6 | Alerta de servicios sobre su cupo | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA7 | Alerta de servicios activos sin coordinador activo | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA8 | Sin situaciones, no aparece ninguna alerta | ✅ | `CU10TableroIntegrationTests` |
| CU10-FA9 | Gráficos: agrupa desde 6 rotaciones y las universidades en «Otras» | ✅ | `CU10TableroIntegrationTests` |
| CU10-EX1 | Filtro desconocido: 400 | ✅ | `CU10TableroIntegrationTests` |
| CU10-EX2 | Periodo mal formado o `asignaturaId` no numérico: 400 | ✅ | `CU10TableroIntegrationTests` |

### CU-11 Consultar el historial (RF-35, RF-36)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU11-FP1 | Editar deja quién, qué campos y valores antes y después | ✅ | `registraCreacionYCambiosConDatosAnterioresYNuevos` |
| CU11-FP2 | El detalle muestra nombre y correo del responsable | ✅ | `CU11HistorialIntegrationTests` |
| CU11-FA1 | Cada módulo registra crear, actualizar, estado y eliminar | ✅ | `CU11HistorialIntegrationTests` |
| CU11-FA2 | Busca por usuario, registro o detalle y filtra por módulo, acción y fechas | ✅ | `CU11HistorialIntegrationTests` |
| CU11-FA3 | Fechas interpretadas en Bogotá e inclusivas | ✅ | `fechaHistorialSeInterpretaEnBogotaYEsInclusiva` |
| CU11-EX1 | Ningún movimiento contiene contraseñas ni hashes, en todos los flujos de usuario | ✅ | `CU11HistorialIntegrationTests` |
| CU11-EX2 | Movimiento inexistente: 404 | ✅ | `CU11HistorialIntegrationTests` |
| CU11-EX3 | Operación rechazada no deja movimiento | ✅ | `CU11HistorialIntegrationTests` |

### CU-12 Consultar residentes y tablero del servicio (RF-34, RNF-11)

| ID | Flujo y resultado esperado | Estado | Prueba actual |
|---|---|---|---|
| CU12-FP1 | Lista residentes en curso y próximos de su servicio | ✅ | `elCoordinadorVeLosResidentesQueRotanHoy...` |
| CU12-FP2 | Tablero con las cifras de su servicio | ✅ | `CU12CoordinadorIntegrationTests` |
| CU12-FA1 | Servicio sin residentes: listas vacías sin error | ✅ | `CU12CoordinadorIntegrationTests` |
| CU12-FA2 | Rotaciones terminadas no aparecen | ✅ | `CU12CoordinadorIntegrationTests` |
| CU12-EX1 | **Aislamiento en residentes:** nada de otros servicios | ✅ | `CU12CoordinadorIntegrationTests` |
| CU12-EX2 | **Aislamiento en tablero:** cifras sin otros servicios, incluso con nombres parecidos | ✅ | `CU12CoordinadorIntegrationTests` |
| CU12-EX3 | Parámetros como `?servicio=otro` no amplían el acceso | ✅ | `CoordinadorServiceImplTest` |
| CU12-EX4 | Sin vínculo activo: 403 con mensaje | ✅ | `CU12CoordinadorIntegrationTests`, `CoordinadorServiceImplTest` |
| CU12-EX5 | Administrador en `/coordinador/**`: 403 | ✅ | `elCoordinadorVeLosResidentes...` |

## 5. Pruebas transversales

| ID | Qué comprueba | Estado |
|---|---|---|
| T-01 | Matriz parametrizada: **cada endpoint** × anónimo, coordinador y administrador, con el código esperado | ✅ | `T01PermisosPorRolH2IntegrationTests` 
| T-02 | El coordinador no puede modificar nada, incluso con solicitud directa | ✅ | `T01PermisosPorRolH2IntegrationTests` 
| T-03 | `ApiExceptionHandler`: JSON mal formado, tipo incorrecto, violación de integridad y error de base; sin trazas ni SQL en la respuesta | ✅ | `ApiExceptionHandlerTest` 
| T-04 | Restricciones de `RBD-04`, `RBD-05` y `RBD-07` directamente en PostgreSQL y H2 | ✅ | `T04RestriccionesBaseIntegrationTests` 
| T-05 | Inyección SQL en búsquedas | ✅ |

**Pruebas unitarias nuevas** (sin Spring, con casos de borde): `LimiteIntentosInicioSesion`,
`CrucesDashboard`, `OcupacionDashboard`, `ProgramasDeConvenio`, `CargaInicialPlan`, `Calendario` y
`DashboardFiltros`.

## 6. Fuera de JUnit

| Tema | Dónde va |
|---|---|
| Archivo de 5 MiB, 5000 filas por hoja, fórmulas, plantilla y asistente de nueve pasos (RF-17) | Vitest: viven en `frontend/src/app/shared/excel` |
| Reporte de Excel (RF-33), contraseña sugerida (RF-04), redirección al panel (RF-01) | Vitest |
| Desempeño (RD-01 a RD-06) | Medición aparte en el ambiente de pruebas, como exige el SRS 3.3 |

## 7. Cómo se ejecuta

Los comandos y la explicación de los reportes están en `backend/README.md`, sección «Pruebas».

**Convenciones:**

- Las pruebas por caso de uso llevan `@DisplayName("CU07-EX6 ...")`, para que el reporte sirva de evidencia y de trazabilidad.
- Las de atomicidad y concurrencia no llevan `@Transactional`: necesitan confirmar de verdad, y no dejan datos en la base.
- Si una prueba descubre un error real, se corrige el producto, no la prueba.
