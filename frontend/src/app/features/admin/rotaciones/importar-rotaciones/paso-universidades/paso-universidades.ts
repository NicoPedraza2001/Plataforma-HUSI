import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { UniversidadDetalle } from '../../../../../models/universidad.model';
import { mensajeError } from '../../../../../services/api-error';
import { UniversidadService } from '../../../../../services/universidad.service';
import { consultarTodasLasPaginas } from '../../../../../shared/consulta/todas-las-paginas';
import { FocoModal } from '../../../../../shared/foco-modal/foco-modal';
import { FormularioUniversidad } from '../../../universidades/formulario-universidad/formulario-universidad';
import { ConfirmarExclusion } from '../confirmar-exclusion/confirmar-exclusion';
import {
  actualizarFilas,
  estaPendiente,
  excluirFilas,
  GrupoEmparejamiento,
  indexarPorNumeroDeFila,
  procesarEnLote,
  contar,
  segunCantidad,
} from '../emparejamiento';
import {
  AsociacionElegida,
  TablaEmparejamiento,
} from '../tabla-emparejamiento/tabla-emparejamiento';
import { emparejarUniversidades } from './emparejar-universidades';

/**
 * Paso 3: universidades del Excel frente a las registradas. Permite crear las nuevas,
 * asociar las mal escritas a una existente, activar inactivas o excluir sus filas.
 */
@Component({
  selector: 'app-paso-universidades',
  imports: [TablaEmparejamiento, FormularioUniversidad, FocoModal, ConfirmarExclusion],
  templateUrl: './paso-universidades.html',
  styleUrl: './paso-universidades.css',
})
export class PasoUniversidades {
  private readonly universidadService = inject(UniversidadService);

  readonly filas = input.required<FilaCargaInicial[]>();
  readonly filasOriginales = input.required<FilaCargaInicial[]>();
  readonly filasCambiadas = output<FilaCargaInicial[]>();
  readonly pendientesCambiados = output<number>();
  readonly trabajandoCambiado = output<boolean>();
  readonly anterior = output<void>();
  readonly siguiente = output<void>();

  protected readonly universidadesDelSistema = signal<UniversidadDetalle[]>([]);
  protected readonly cargandoUniversidades = signal(true);
  protected readonly errorDeCarga = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly contar = contar;
  protected readonly grupoPorExcluir = signal<GrupoEmparejamiento | null>(null);

  // Formulario de creación (reutiliza FormularioUniversidad del módulo de universidades).
  protected readonly grupoEnCreacion = signal<GrupoEmparejamiento | null>(null);
  protected readonly nombreNuevaUniversidad = signal('');
  protected readonly paisNuevaUniversidad = signal('');
  protected readonly perfilNuevaUniversidad = signal('');
  protected readonly errorFormulario = signal('');

  protected readonly grupos = computed(() =>
    emparejarUniversidades(
      this.filas(),
      indexarPorNumeroDeFila(this.filasOriginales()),
      this.universidadesDelSistema(),
    ),
  );
  protected readonly totalPendientes = computed(() => this.grupos().filter(estaPendiente).length);
  protected readonly totalNuevas = computed(
    () => this.grupos().filter((grupo) => grupo.estado === 'nuevo').length,
  );
  /** Nuevas sin parecido con ninguna registrada y con un único país: se pueden crear sin revisar. */
  protected readonly gruposParaCrearEnLote = computed(() =>
    this.grupos().filter(
      (grupo) =>
        grupo.estado === 'nuevo' &&
        !grupo.sugerencias.length &&
        !!grupo.contexto &&
        !grupo.contexto.includes(','),
    ),
  );

  constructor() {
    effect(() => {
      if (!this.cargandoUniversidades() && !this.errorDeCarga()) {
        this.pendientesCambiados.emit(this.totalPendientes());
      }
    });
    effect(() => this.trabajandoCambiado.emit(this.guardando()));
    this.cargarUniversidades();
  }

  protected async cargarUniversidades() {
    this.cargandoUniversidades.set(true);
    this.errorDeCarga.set('');
    try {
      const universidades = await consultarTodasLasPaginas((pagina, tamano) =>
        this.universidadService.buscar({ pagina, tamano }),
      );
      this.universidadesDelSistema.set(universidades);
    } catch (e) {
      this.errorDeCarga.set(mensajeError(e));
    } finally {
      this.cargandoUniversidades.set(false);
    }
  }

  // ---------- Crear ----------

  protected abrirCreacion(grupo: GrupoEmparejamiento) {
    this.nombreNuevaUniversidad.set(grupo.valorEnExcel);
    this.paisNuevaUniversidad.set(grupo.contexto.split(',')[0]?.trim() ?? '');
    this.perfilNuevaUniversidad.set('');
    this.errorFormulario.set('');
    this.grupoEnCreacion.set(grupo);
  }

  protected cerrarCreacion() {
    if (!this.guardando()) this.grupoEnCreacion.set(null);
  }

  protected async guardarUniversidadNueva() {
    const grupo = this.grupoEnCreacion();
    if (!grupo || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      const universidadCreada = await this.crearUniversidad(
        this.nombreNuevaUniversidad(),
        this.paisNuevaUniversidad(),
        this.perfilNuevaUniversidad(),
      );
      this.filasCambiadas.emit(
        this.aplicarUniversidadEnFilas(this.filas(), grupo, universidadCreada),
      );
      this.grupoEnCreacion.set(null);
      this.mostrarAviso(
        `Universidad «${universidadCreada.nombre}» creada y asignada a ${contar(grupo.numerosFila.length, 'fila', 'filas')}.`,
      );
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async crearUniversidadesEnLote() {
    const gruposACrear = this.gruposParaCrearEnLote();
    if (!gruposACrear.length || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    let filasActualizadas = this.filas();
    const { procesados, errores } = await procesarEnLote(
      gruposACrear,
      async (grupo) => {
        const universidadCreada = await this.crearUniversidad(
          grupo.valorEnExcel,
          grupo.contexto,
          '',
        );
        filasActualizadas = this.aplicarUniversidadEnFilas(
          filasActualizadas,
          grupo,
          universidadCreada,
        );
      },
      (grupo) => grupo.valorEnExcel,
      mensajeError,
    );
    this.filasCambiadas.emit(filasActualizadas);
    this.guardando.set(false);
    this.mostrarAviso(
      `${segunCantidad(procesados, 'Se creó', 'Se crearon')} ${contar(procesados, 'universidad', 'universidades')}.`,
    );
    if (errores.length) this.error.set('No se pudieron crear: ' + errores.join(' · '));
  }

  private async crearUniversidad(nombre: string, pais: string, perfil: string) {
    const universidadCreada = await firstValueFrom(
      this.universidadService.crear({
        nombre: nombre.trim(),
        pais: pais.trim(),
        perfil: perfil.trim() || null,
      }),
    );
    this.reemplazarUniversidadEnCatalogo(universidadCreada);
    return universidadCreada;
  }

  // ---------- Asociar, usar datos del sistema, activar ----------

  protected asociarUniversidadExistente({ grupo, registroId }: AsociacionElegida) {
    const universidadElegida = this.buscarUniversidadPorId(registroId);
    if (!universidadElegida) return;
    this.filasCambiadas.emit(
      this.aplicarUniversidadEnFilas(this.filas(), grupo, universidadElegida),
    );
    this.mostrarAviso(
      `${contar(grupo.numerosFila.length, 'fila', 'filas')} ${segunCantidad(grupo.numerosFila.length, 'ahora usa', 'ahora usan')} «${universidadElegida.nombre}».`,
    );
  }

  protected usarDatosDelSistema(grupo: GrupoEmparejamiento) {
    if (grupo.registro) this.asociarUniversidadExistente({ grupo, registroId: grupo.registro.id });
  }

  protected async activarUniversidad(grupo: GrupoEmparejamiento) {
    if (!grupo.registro || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      const universidadActivada = await firstValueFrom(
        this.universidadService.cambiarEstado(grupo.registro.id, 'Activo'),
      );
      this.reemplazarUniversidadEnCatalogo(universidadActivada);
      this.mostrarAviso(`Universidad «${universidadActivada.nombre}» activada.`);
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
      `${segunCantidad(grupo.numerosFila.length, 'Se excluyó', 'Se excluyeron')} ${contar(grupo.numerosFila.length, 'fila', 'filas')} de la importación.`,
    );
  }

  // ---------- Auxiliares ----------

  /** Las filas del grupo pasan a usar el nombre y el país exactos del registro. */
  private aplicarUniversidadEnFilas(
    filas: FilaCargaInicial[],
    grupo: GrupoEmparejamiento,
    universidad: UniversidadDetalle,
  ): FilaCargaInicial[] {
    return actualizarFilas(filas, grupo.numerosFila, {
      universidad: universidad.nombre,
      pais: universidad.pais,
    });
  }

  private buscarUniversidadPorId(universidadId: number): UniversidadDetalle | undefined {
    return this.universidadesDelSistema().find((universidad) => universidad.id === universidadId);
  }

  private reemplazarUniversidadEnCatalogo(universidadActualizada: UniversidadDetalle) {
    this.universidadesDelSistema.update((universidades) => [
      ...universidades.filter((universidad) => universidad.id !== universidadActualizada.id),
      universidadActualizada,
    ]);
  }

  private mostrarAviso(texto: string) {
    this.error.set('');
    this.aviso.set(texto);
  }
}
