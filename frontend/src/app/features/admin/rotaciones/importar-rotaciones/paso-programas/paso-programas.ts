import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { Programa } from '../../../../../models/programa.model';
import { UniversidadDetalle } from '../../../../../models/universidad.model';
import { mensajeError } from '../../../../../services/api-error';
import { ProgramaService } from '../../../../../services/programa.service';
import { UniversidadService } from '../../../../../services/universidad.service';
import { consultarTodasLasPaginas } from '../../../../../shared/consulta/todas-las-paginas';
import { FocoModal } from '../../../../../shared/foco-modal/foco-modal';
import { FormularioPrograma } from '../../../programas/formulario-programa/formulario-programa';
import { convenioSegunPais, esUniversidadPropia } from '../../../programas/programas-de-convenio';
import { ConfirmarExclusion } from '../confirmar-exclusion/confirmar-exclusion';
import {
  actualizarFilas,
  estaPendiente,
  excluirFilas,
  GrupoEmparejamiento,
  indexarPorNumeroDeFila,
  procesarEnLote,
  cantidadConPalabra,
  segunCantidad,
} from '../emparejamiento';
import {
  AsociacionElegida,
  TablaEmparejamiento,
} from '../tabla-emparejamiento/tabla-emparejamiento';
import { buscarProgramaDeUniversidad, emparejarProgramas } from './emparejar-programas';

/**
 * Paso 4: programas del Excel frente a los registrados en cada universidad. Mismas
 * acciones que el paso de universidades: crear, asociar a uno existente, activar o excluir.
 */
@Component({
  selector: 'app-paso-programas',
  imports: [TablaEmparejamiento, FormularioPrograma, FocoModal, ConfirmarExclusion],
  templateUrl: './paso-programas.html',
  styleUrl: './paso-programas.css',
})
export class PasoProgramas {
  private readonly universidadService = inject(UniversidadService);
  private readonly programaService = inject(ProgramaService);

  readonly filas = input.required<FilaCargaInicial[]>();
  readonly filasOriginales = input.required<FilaCargaInicial[]>();
  readonly filasCambiadas = output<FilaCargaInicial[]>();
  readonly pendientesCambiados = output<number>();
  readonly trabajandoCambiado = output<boolean>();
  readonly anterior = output<void>();
  readonly siguiente = output<void>();

  protected readonly universidadesDelSistema = signal<UniversidadDetalle[]>([]);
  protected readonly programasDelSistema = signal<Programa[]>([]);
  protected readonly cargandoCatalogos = signal(true);
  protected readonly errorDeCarga = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly cantidadConPalabra = cantidadConPalabra;
  protected readonly grupoPorExcluir = signal<GrupoEmparejamiento | null>(null);

  // Formulario de creación (reutiliza FormularioPrograma del módulo de programas).
  protected readonly grupoEnCreacion = signal<GrupoEmparejamiento | null>(null);
  protected readonly nombreNuevoPrograma = signal('');
  protected readonly universidadIdNuevoPrograma = signal<number | null>(null);
  protected readonly errorFormulario = signal('');

  protected readonly universidadesActivas = computed(() =>
    this.universidadesDelSistema().filter((universidad) => universidad.estado === 'Activo'),
  );
  protected readonly grupos = computed(() =>
    emparejarProgramas(
      this.filas(),
      indexarPorNumeroDeFila(this.filasOriginales()),
      this.universidadesDelSistema(),
      this.programasDelSistema(),
    ),
  );
  protected readonly totalPendientes = computed(() => this.grupos().filter(estaPendiente).length);
  protected readonly totalNuevos = computed(
    () => this.grupos().filter((grupo) => grupo.estado === 'nuevo').length,
  );
  /**
   * Nuevos que se pueden resolver sin revisar: los de la Javeriana sin parecido con ningún
   * programa registrado y todos los de universidades externas (van a su convenio).
   */
  protected readonly gruposParaCrearEnLote = computed(() =>
    this.grupos().filter(
      (grupo) =>
        grupo.estado === 'nuevo' &&
        (!grupo.sugerencias.length || !!this.universidadExternaDelGrupo(grupo)),
    ),
  );

  constructor() {
    effect(() => {
      if (!this.cargandoCatalogos() && !this.errorDeCarga()) {
        this.pendientesCambiados.emit(this.totalPendientes());
      }
    });
    effect(() => this.trabajandoCambiado.emit(this.guardando()));
    this.cargarCatalogos();
  }

  protected async cargarCatalogos() {
    this.cargandoCatalogos.set(true);
    this.errorDeCarga.set('');
    try {
      const [universidades, programas] = await Promise.all([
        consultarTodasLasPaginas((pagina, tamano) =>
          this.universidadService.buscar({ pagina, tamano }),
        ),
        consultarTodasLasPaginas((pagina, tamano) =>
          this.programaService.buscar({ pagina, tamano }),
        ),
      ]);
      this.universidadesDelSistema.set(universidades);
      this.programasDelSistema.set(programas);
    } catch (e) {
      this.errorDeCarga.set(mensajeError(e));
    } finally {
      this.cargandoCatalogos.set(false);
    }
  }

  // ---------- Crear ----------

  protected abrirCreacion(grupo: GrupoEmparejamiento) {
    this.nombreNuevoPrograma.set(grupo.valorEnExcel);
    this.universidadIdNuevoPrograma.set(grupo.contextoId);
    this.errorFormulario.set('');
    this.grupoEnCreacion.set(grupo);
  }

  protected cerrarCreacion() {
    if (!this.guardando()) this.grupoEnCreacion.set(null);
  }

  protected async guardarProgramaNuevo() {
    const grupo = this.grupoEnCreacion();
    const universidadId = this.universidadIdNuevoPrograma();
    if (!grupo || !universidadId || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      const programa = await this.reutilizarOCrearPrograma(
        this.nombreNuevoPrograma(),
        universidadId,
      );
      this.filasCambiadas.emit(this.aplicarProgramaEnFilas(this.filas(), grupo, programa));
      this.grupoEnCreacion.set(null);
      this.mostrarAviso(
        `Programa «${programa.nombre}» asignado a ${cantidadConPalabra(grupo.numerosFila.length, 'fila', 'filas')}.`,
      );
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async crearProgramasEnLote() {
    const gruposACrear = this.gruposParaCrearEnLote();
    if (!gruposACrear.length || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    let filasActualizadas = this.filas();
    const { procesados, errores } = await procesarEnLote(
      gruposACrear,
      async (grupo) => {
        const universidadExterna = this.universidadExternaDelGrupo(grupo);
        const nombrePrograma = universidadExterna
          ? convenioSegunPais(universidadExterna.pais)
          : grupo.valorEnExcel;
        const programa = await this.reutilizarOCrearPrograma(nombrePrograma, grupo.contextoId!);
        filasActualizadas = this.aplicarProgramaEnFilas(filasActualizadas, grupo, programa);
      },
      (grupo) => `${grupo.valorEnExcel} (${grupo.contexto})`,
      mensajeError,
    );
    this.filasCambiadas.emit(filasActualizadas);
    this.guardando.set(false);
    this.mostrarAviso(
      `${segunCantidad(procesados, 'Se resolvió', 'Se resolvieron')} ${cantidadConPalabra(procesados, 'programa', 'programas')}.`,
    );
    if (errores.length) this.error.set('No se pudieron crear: ' + errores.join(' · '));
  }

  /**
   * Varios programas del Excel de una misma universidad externa terminan en el mismo
   * convenio: si ese programa ya está activo en la universidad se reutiliza en vez de crearlo.
   */
  private async reutilizarOCrearPrograma(nombre: string, universidadId: number) {
    const programaRegistrado = buscarProgramaDeUniversidad(
      this.programasDelSistema(),
      universidadId,
      nombre,
    );
    if (programaRegistrado?.estado === 'Activo') return programaRegistrado;
    const programaCreado = await firstValueFrom(
      this.programaService.crear({ nombre: nombre.trim(), universidadId }),
    );
    this.reemplazarProgramaEnCatalogo(programaCreado);
    return programaCreado;
  }

  // ---------- Asociar, activar ----------

  protected asociarProgramaExistente({ grupo, registroId }: AsociacionElegida) {
    const programaElegido = this.programasDelSistema().find(
      (programa) => programa.id === registroId,
    );
    if (!programaElegido) return;
    this.filasCambiadas.emit(this.aplicarProgramaEnFilas(this.filas(), grupo, programaElegido));
    this.mostrarAviso(
      `${cantidadConPalabra(grupo.numerosFila.length, 'fila', 'filas')} ${segunCantidad(grupo.numerosFila.length, 'ahora usa', 'ahora usan')} «${programaElegido.nombre}».`,
    );
  }

  protected async activarPrograma(grupo: GrupoEmparejamiento) {
    if (!grupo.registro || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      const programaActivado = await firstValueFrom(
        this.programaService.cambiarEstado(grupo.registro.id, 'Activo'),
      );
      this.reemplazarProgramaEnCatalogo(programaActivado);
      this.mostrarAviso(`Programa «${programaActivado.nombre}» activado.`);
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  // ---------- Excluir ----------

  protected confirmarExclusion() {
    const grupo = this.grupoPorExcluir();
    if (!grupo) return;
    this.filasCambiadas.emit(excluirFilas(this.filas(), grupo.numerosFila));
    this.grupoPorExcluir.set(null);
    this.mostrarAviso(
      `${segunCantidad(grupo.numerosFila.length, 'Se excluyó', 'Se excluyeron')} ${cantidadConPalabra(grupo.numerosFila.length, 'fila', 'filas')} de la importación.`,
    );
  }

  // ---------- Auxiliares ----------

  /** Universidad del grupo si es externa a la Javeriana; null si es la Javeriana o no existe. */
  private universidadExternaDelGrupo(grupo: GrupoEmparejamiento): UniversidadDetalle | null {
    const universidad = this.universidadesDelSistema().find(
      (registrada) => registrada.id === grupo.contextoId,
    );
    return universidad && !esUniversidadPropia(universidad.nombre) ? universidad : null;
  }

  /** Las filas del grupo pasan a usar el nombre exacto del programa y de su universidad. */
  private aplicarProgramaEnFilas(
    filas: FilaCargaInicial[],
    grupo: GrupoEmparejamiento,
    programa: Programa,
  ): FilaCargaInicial[] {
    return actualizarFilas(filas, grupo.numerosFila, {
      programa: programa.nombre,
      universidad: programa.universidad,
    });
  }

  private reemplazarProgramaEnCatalogo(programaActualizado: Programa) {
    this.programasDelSistema.update((programas) => [
      ...programas.filter((programa) => programa.id !== programaActualizado.id),
      programaActualizado,
    ]);
  }

  private mostrarAviso(texto: string) {
    this.error.set('');
    this.aviso.set(texto);
  }
}
