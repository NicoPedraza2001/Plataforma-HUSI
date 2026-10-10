import { Component, computed, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { OpcionCatalogo } from '../../../../../models/opcion-catalogo.model';
import { Programa } from '../../../../../models/programa.model';
import { ResidenteDetalle, ResidenteRequest } from '../../../../../models/residente.model';
import { TipoDocumento } from '../../../../../models/tipo-documento.model';
import { UniversidadDetalle } from '../../../../../models/universidad.model';
import { mensajeError } from '../../../../../services/api-error';
import { PerfilAcademicoService } from '../../../../../services/perfil-academico.service';
import { ProgramaService } from '../../../../../services/programa.service';
import { ResidenteService } from '../../../../../services/residente.service';
import { TipoDocumentoService } from '../../../../../services/tipo-documento.service';
import { UniversidadService } from '../../../../../services/universidad.service';
import { consultarTodasLasPaginas } from '../../../../../shared/consulta/todas-las-paginas';
import { FocoModal } from '../../../../../shared/foco-modal/foco-modal';
import { Paginacion } from '../../../../../shared/paginacion/paginacion';
import { FormularioResidente } from '../../../residentes/formulario-residente/formulario-residente';
import { ConfirmarExclusion } from '../confirmar-exclusion/confirmar-exclusion';
import {
  actualizarFilas,
  CLASE_BADGE_ESTADO,
  describirFilas,
  esMismoTexto,
  estaPendiente,
  ETIQUETA_ESTADO,
  excluirFilas,
  OPCIONES_FILTRO_ESTADO,
  procesarEnLote,
  cantidadConPalabra,
  segunCantidad,
} from '../emparejamiento';
import { FiltroDeTabla } from '../filtro-de-tabla';
import {
  claveDeDocumento,
  describirPrograma,
  prepararDatosFormulario,
  RevisionResidente,
  revisarResidentes,
  tieneDocumentoRepetido,
  VarianteResidente,
} from './revisar-residentes';

/** Documentos por consulta al backend (acepta hasta 5000; se parte para no enviar cuerpos enormes). */
const DOCUMENTOS_POR_CONSULTA = 1000;

/**
 * Paso 7: residentes del Excel frente a la base de datos, buscados por documento.
 * Permite crear los nuevos, actualizar los existentes a los que les falta o difiere un dato,
 * usar los datos ya registrados, activar inactivos o excluir sus filas. El tipo de documento,
 * el perfil y el programa se eligen de los catálogos, nunca se escriben a mano.
 */
@Component({
  selector: 'app-paso-residentes',
  imports: [FormsModule, Paginacion, FocoModal, FormularioResidente, ConfirmarExclusion],
  templateUrl: './paso-residentes.html',
  styleUrl: './paso-residentes.css',
})
export class PasoResidentes implements OnInit {
  private readonly residenteService = inject(ResidenteService);
  private readonly programaService = inject(ProgramaService);
  private readonly universidadService = inject(UniversidadService);
  private readonly tipoDocumentoService = inject(TipoDocumentoService);
  private readonly perfilAcademicoService = inject(PerfilAcademicoService);

  readonly filas = input.required<FilaCargaInicial[]>();
  readonly filasCambiadas = output<FilaCargaInicial[]>();
  readonly pendientesCambiados = output<number>();
  readonly trabajandoCambiado = output<boolean>();
  readonly anterior = output<void>();
  readonly siguiente = output<void>();

  // Catálogos y residentes registrados
  protected readonly tiposDocumento = signal<TipoDocumento[]>([]);
  protected readonly perfilesAcademicos = signal<OpcionCatalogo[]>([]);
  protected readonly programasDelSistema = signal<Programa[]>([]);
  protected readonly universidadesDelSistema = signal<UniversidadDetalle[]>([]);
  protected readonly residentesPorDocumento = signal<ReadonlyMap<string, ResidenteDetalle>>(
    new Map(),
  );
  protected readonly cargandoDatos = signal(true);
  protected readonly errorDeCarga = signal('');

  // Estado de la pantalla
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly cantidadConPalabra = cantidadConPalabra;
  protected readonly revisionPorExcluir = signal<RevisionResidente | null>(null);
  protected readonly tieneDocumentoRepetido = tieneDocumentoRepetido;

  // Documento repetido con datos distintos (se identifica por la clave del documento)
  private readonly claveDocumentoRepetido = signal<string | null>(null);
  /** Se cierra solo cuando el documento deja de tener datos distintos. */
  protected readonly revisionDocumentoRepetido = computed(() => {
    const clave = this.claveDocumentoRepetido();
    const revision = this.revisiones().find((revisionActual) => revisionActual.clave === clave);
    return revision && tieneDocumentoRepetido(revision) ? revision : null;
  });
  /** Grupo de filas al que se le está escribiendo un documento nuevo (su primera fila). */
  protected readonly varianteCambiandoDocumento = signal<number | null>(null);
  protected readonly documentoNuevo = signal('');
  protected readonly errorDocumentoRepetido = signal('');

  // Formulario (reutiliza FormularioResidente del módulo de residentes)
  protected readonly revisionEnEdicion = signal<RevisionResidente | null>(null);
  protected readonly datosFormulario = signal<ResidenteRequest>(this.crearDatosVacios());
  protected readonly errorFormulario = signal('');

  protected readonly opcionesFiltro = OPCIONES_FILTRO_ESTADO;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly claseBadgeEstado = CLASE_BADGE_ESTADO;
  protected readonly describirFilas = describirFilas;
  protected readonly describirPrograma = describirPrograma;

  protected readonly revisiones = computed(() =>
    revisarResidentes(
      this.filas(),
      {
        tiposDocumento: this.tiposDocumento(),
        perfilesAcademicos: this.perfilesAcademicos(),
        programas: this.programasDelSistema(),
      },
      this.residentesPorDocumento(),
    ),
  );
  protected readonly filtro = new FiltroDeTabla(
    () => this.revisiones(),
    (revision) =>
      `${revision.datosEnExcel.nombre} ${revision.datosEnExcel.documento} ${revision.datosEnExcel.programa}`,
  );
  protected readonly totalPendientes = computed(
    () => this.revisiones().filter(estaPendiente).length,
  );
  protected readonly revisionesNuevas = computed(() =>
    this.revisiones().filter((revision) => revision.estado === 'nuevo'),
  );
  protected readonly totalExistentes = computed(
    () => this.revisiones().filter((revision) => revision.residenteRegistrado).length,
  );
  /** Programas para el formulario; incluye el actual del residente aunque esté inactivo. */
  protected readonly opcionesDePrograma = computed<OpcionCatalogo[]>(() => {
    const programaActualId = this.revisionEnEdicion()?.residenteRegistrado?.programaId;
    return this.programasDelSistema()
      .filter((programa) => programa.estado === 'Activo' || programa.id === programaActualId)
      .map((programa) => ({ id: programa.id, nombre: describirPrograma(programa) }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));
  });

  constructor() {
    effect(() => {
      if (!this.cargandoDatos() && !this.errorDeCarga()) {
        this.pendientesCambiados.emit(this.totalPendientes());
      }
    });
    effect(() => this.trabajandoCambiado.emit(this.guardando()));
  }

  // Se carga en ngOnInit y no en el constructor porque necesita las filas (un input).
  ngOnInit() {
    this.cargarDatos();
  }

  // ---------- Carga ----------

  protected async cargarDatos() {
    this.cargandoDatos.set(true);
    this.errorDeCarga.set('');
    try {
      const [tiposDocumento, perfilesAcademicos, programas, universidades, residentes] =
        await Promise.all([
          firstValueFrom(this.tipoDocumentoService.listar()),
          firstValueFrom(this.perfilAcademicoService.listar()),
          consultarTodasLasPaginas((pagina, tamano) =>
            this.programaService.buscar({ pagina, tamano }),
          ),
          consultarTodasLasPaginas((pagina, tamano) =>
            this.universidadService.buscar({ pagina, tamano }),
          ),
          this.buscarResidentesRegistrados(),
        ]);
      this.tiposDocumento.set(tiposDocumento);
      this.perfilesAcademicos.set(perfilesAcademicos);
      this.programasDelSistema.set(programas);
      this.universidadesDelSistema.set(universidades);
      this.residentesPorDocumento.set(
        new Map(
          residentes.map((residente) => [claveDeDocumento(residente.numeroDocumento), residente]),
        ),
      );
    } catch (e) {
      this.errorDeCarga.set(mensajeError(e));
    } finally {
      this.cargandoDatos.set(false);
    }
  }

  /** Busca en bloque los documentos del Excel en lugar de consultar uno por uno. */
  private async buscarResidentesRegistrados(): Promise<ResidenteDetalle[]> {
    const documentosDelExcel = [
      ...new Set(
        this.filas()
          .map((fila) => fila.documento.trim())
          .filter(Boolean),
      ),
    ];
    const residentes: ResidenteDetalle[] = [];
    for (let inicio = 0; inicio < documentosDelExcel.length; inicio += DOCUMENTOS_POR_CONSULTA) {
      const documentosDelBloque = documentosDelExcel.slice(
        inicio,
        inicio + DOCUMENTOS_POR_CONSULTA,
      );
      residentes.push(
        ...(await firstValueFrom(this.residenteService.buscarPorDocumentos(documentosDelBloque))),
      );
    }
    return residentes;
  }

  // ---------- Crear o actualizar desde el formulario ----------

  protected abrirFormulario(revision: RevisionResidente) {
    this.datosFormulario.set(prepararDatosFormulario(revision));
    this.errorFormulario.set('');
    this.revisionEnEdicion.set(revision);
  }

  protected cerrarFormulario() {
    if (!this.guardando()) this.revisionEnEdicion.set(null);
  }

  protected async guardarResidente() {
    const revision = this.revisionEnEdicion();
    if (!revision || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      const residenteGuardado = revision.residenteRegistrado
        ? await this.actualizarResidente(revision.residenteRegistrado, this.datosFormulario())
        : await this.crearResidente(this.datosFormulario());
      this.filasCambiadas.emit(
        this.aplicarResidenteEnFilas(this.filas(), revision.numerosFila, residenteGuardado),
      );
      this.revisionEnEdicion.set(null);
      this.mostrarAviso(
        `Residente «${residenteGuardado.nombre}» ${revision.residenteRegistrado ? 'actualizado' : 'creado'}.`,
      );
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async crearResidentesNuevosEnLote() {
    const revisionesACrear = this.revisionesNuevas();
    if (!revisionesACrear.length || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    let filasActualizadas = this.filas();
    const { procesados, errores } = await procesarEnLote(
      revisionesACrear,
      async (revision) => {
        const residenteCreado = await this.crearResidente(prepararDatosFormulario(revision));
        filasActualizadas = this.aplicarResidenteEnFilas(
          filasActualizadas,
          revision.numerosFila,
          residenteCreado,
        );
      },
      (revision) => revision.datosEnExcel.documento,
      mensajeError,
    );
    this.filasCambiadas.emit(filasActualizadas);
    this.guardando.set(false);
    this.mostrarAviso(
      `${segunCantidad(procesados, 'Se creó', 'Se crearon')} ${cantidadConPalabra(procesados, 'residente', 'residentes')}.`,
    );
    if (errores.length) this.error.set('No se pudieron crear: ' + errores.join(' · '));
  }

  private async crearResidente(datos: ResidenteRequest): Promise<ResidenteDetalle> {
    const residenteCreado = await firstValueFrom(
      this.residenteService.crear(this.limpiarDatos(datos)),
    );
    this.registrarResidente(residenteCreado);
    return residenteCreado;
  }

  private async actualizarResidente(
    residenteRegistrado: ResidenteDetalle,
    datos: ResidenteRequest,
  ): Promise<ResidenteDetalle> {
    const residenteActualizado = await firstValueFrom(
      this.residenteService.actualizar(residenteRegistrado.id, this.limpiarDatos(datos)),
    );
    this.registrarResidente(residenteActualizado, residenteRegistrado.numeroDocumento);
    return residenteActualizado;
  }

  // ---------- Documento repetido con datos distintos ----------

  protected abrirDocumentoRepetido(revision: RevisionResidente) {
    this.cancelarCambioDeDocumento();
    this.claveDocumentoRepetido.set(revision.clave);
  }

  protected cerrarDocumentoRepetido() {
    if (this.guardando()) return;
    this.cancelarCambioDeDocumento();
    this.claveDocumentoRepetido.set(null);
  }

  /** Es la misma persona: todas las filas del documento pasan a usar los datos de este grupo. */
  protected usarVarianteEnTodasLasFilas(revision: RevisionResidente, variante: VarianteResidente) {
    const filaModelo = this.filas().find((fila) => fila.fila === variante.numerosFila[0]);
    if (!filaModelo) return;
    this.filasCambiadas.emit(
      actualizarFilas(this.filas(), revision.numerosFila, {
        residente: filaModelo.residente,
        tipoDocumento: filaModelo.tipoDocumento,
        perfilAcademico: filaModelo.perfilAcademico,
        programa: filaModelo.programa,
        universidad: filaModelo.universidad,
        pais: filaModelo.pais,
      }),
    );
    this.mostrarAviso(
      `Las filas ${describirFilas(revision.numerosFila)} ahora son de «${variante.datos.nombre}».`,
    );
  }

  protected empezarCambioDeDocumento(variante: VarianteResidente) {
    this.errorDocumentoRepetido.set('');
    this.documentoNuevo.set('');
    this.varianteCambiandoDocumento.set(variante.numerosFila[0]);
  }

  protected cancelarCambioDeDocumento() {
    this.varianteCambiandoDocumento.set(null);
    this.documentoNuevo.set('');
    this.errorDocumentoRepetido.set('');
  }

  /**
   * Es otra persona: sus filas reciben el documento correcto y se revisan por separado. El
   * documento nuevo se busca en el sistema, porque solo se consultaron los que traía el Excel.
   */
  protected async cambiarDocumentoDeVariante(
    revision: RevisionResidente,
    variante: VarianteResidente,
  ) {
    const documento = this.documentoNuevo().trim();
    if (!documento) {
      this.errorDocumentoRepetido.set('Escribe el número de documento correcto.');
      return;
    }
    if (claveDeDocumento(documento) === revision.clave) {
      this.errorDocumentoRepetido.set('Es el mismo documento; escribe el de la otra persona.');
      return;
    }
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorDocumentoRepetido.set('');
    try {
      const [residenteRegistrado] = await firstValueFrom(
        this.residenteService.buscarPorDocumentos([documento]),
      );
      if (residenteRegistrado) this.registrarResidente(residenteRegistrado);
      this.filasCambiadas.emit(actualizarFilas(this.filas(), variante.numerosFila, { documento }));
      this.cancelarCambioDeDocumento();
      this.mostrarAviso(
        `Las filas ${describirFilas(variante.numerosFila)} de «${variante.datos.nombre}» ahora tienen el documento ${documento}.`,
      );
    } catch (e) {
      this.errorDocumentoRepetido.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  /** Quita de la importación solo las filas de este grupo. */
  protected excluirVariante(revision: RevisionResidente, variante: VarianteResidente) {
    this.revisionPorExcluir.set({
      ...revision,
      datosEnExcel: variante.datos,
      numerosFila: variante.numerosFila,
    });
  }

  // ---------- Usar datos del sistema, activar, excluir ----------

  protected usarDatosDelSistema(revision: RevisionResidente) {
    if (!revision.residenteRegistrado) return;
    this.filasCambiadas.emit(
      this.aplicarResidenteEnFilas(
        this.filas(),
        revision.numerosFila,
        revision.residenteRegistrado,
      ),
    );
    this.mostrarAviso(
      `Las filas de ${revision.residenteRegistrado.nombre} ahora usan los datos del sistema.`,
    );
  }

  protected async activarResidente(revision: RevisionResidente) {
    if (!revision.residenteRegistrado || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      const residenteActivado = await firstValueFrom(
        this.residenteService.cambiarEstado(revision.residenteRegistrado.id, 'Activo'),
      );
      this.registrarResidente(residenteActivado);
      this.mostrarAviso(`Residente «${residenteActivado.nombre}» activado.`);
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected confirmarExclusion() {
    const revision = this.revisionPorExcluir();
    if (!revision) return;
    this.filasCambiadas.emit(excluirFilas(this.filas(), revision.numerosFila));
    this.revisionPorExcluir.set(null);
    this.mostrarAviso(
      `${segunCantidad(revision.numerosFila.length, 'Se excluyó', 'Se excluyeron')} ${cantidadConPalabra(revision.numerosFila.length, 'fila', 'filas')} de la importación.`,
    );
  }

  // ---------- Auxiliares ----------

  protected describirProgramaPorId(programaId: number | null): string {
    return describirPrograma(
      this.programasDelSistema().find((programa) => programa.id === programaId),
    );
  }

  /** Las filas del residente pasan a usar sus datos registrados, incluidos programa y país. */
  private aplicarResidenteEnFilas(
    filas: FilaCargaInicial[],
    numerosFila: number[],
    residente: ResidenteDetalle,
  ): FilaCargaInicial[] {
    const programa = this.programasDelSistema().find(
      (programaRegistrado) => programaRegistrado.id === residente.programaId,
    );
    const universidad = this.universidadesDelSistema().find(
      (universidadRegistrada) =>
        programa && esMismoTexto(universidadRegistrada.nombre, programa.universidad),
    );
    return actualizarFilas(filas, numerosFila, {
      residente: residente.nombre,
      tipoDocumento: residente.tipoDocumento,
      documento: residente.numeroDocumento,
      perfilAcademico: residente.perfilAcademico ?? '',
      ...(programa ? { programa: programa.nombre, universidad: programa.universidad } : {}),
      ...(universidad ? { pais: universidad.pais } : {}),
    });
  }

  /** Guarda el residente en el índice por documento; si cambió el documento, quita el anterior. */
  private registrarResidente(residente: ResidenteDetalle, documentoAnterior?: string) {
    this.residentesPorDocumento.update((residentesActuales) => {
      const residentesActualizados = new Map(residentesActuales);
      if (documentoAnterior) residentesActualizados.delete(claveDeDocumento(documentoAnterior));
      residentesActualizados.set(claveDeDocumento(residente.numeroDocumento), residente);
      return residentesActualizados;
    });
  }

  private limpiarDatos(datos: ResidenteRequest): ResidenteRequest {
    return {
      ...datos,
      nombre: datos.nombre.trim(),
      numeroDocumento: datos.numeroDocumento.trim(),
    };
  }

  private crearDatosVacios(): ResidenteRequest {
    return {
      nombre: '',
      tipoDocumento: '',
      numeroDocumento: '',
      perfilAcademico: '',
      programaId: null,
    };
  }

  private mostrarAviso(texto: string) {
    this.error.set('');
    this.aviso.set(texto);
  }
}
