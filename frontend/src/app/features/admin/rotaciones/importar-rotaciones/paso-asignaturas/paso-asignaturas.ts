import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Asignatura, AsignaturaRequest } from '../../../../../models/asignatura.model';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { OpcionCatalogo } from '../../../../../models/opcion-catalogo.model';
import { Servicio } from '../../../../../models/servicio.model';
import { mensajeError } from '../../../../../services/api-error';
import { AsignaturaService } from '../../../../../services/asignatura.service';
import { ServicioService } from '../../../../../services/servicio.service';
import { consultarTodasLasPaginas } from '../../../../../shared/consulta/todas-las-paginas';
import { FocoModal } from '../../../../../shared/foco-modal/foco-modal';
import { FormularioAsignatura } from '../../../asignaturas/formulario-asignatura/formulario-asignatura';
import { ConfirmarExclusion } from '../confirmar-exclusion/confirmar-exclusion';
import {
  actualizarFilas,
  esMismoTexto,
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
import { emparejarAsignaturas } from './emparejar-asignaturas';

/**
 * Paso 6: asignaturas del Excel frente a las registradas en cada servicio. Mismas
 * acciones que los demás pasos de emparejamiento: crear, asociar a una existente,
 * activar o excluir.
 */
@Component({
  selector: 'app-paso-asignaturas',
  imports: [TablaEmparejamiento, FormularioAsignatura, FocoModal, ConfirmarExclusion],
  templateUrl: './paso-asignaturas.html',
  styleUrl: './paso-asignaturas.css',
})
export class PasoAsignaturas {
  private readonly servicioService = inject(ServicioService);
  private readonly asignaturaService = inject(AsignaturaService);

  readonly filas = input.required<FilaCargaInicial[]>();
  readonly filasOriginales = input.required<FilaCargaInicial[]>();
  readonly filasCambiadas = output<FilaCargaInicial[]>();
  readonly pendientesCambiados = output<number>();
  readonly trabajandoCambiado = output<boolean>();
  readonly anterior = output<void>();
  readonly siguiente = output<void>();

  protected readonly serviciosDelSistema = signal<Servicio[]>([]);
  protected readonly asignaturasDelSistema = signal<Asignatura[]>([]);
  protected readonly cargandoCatalogos = signal(true);
  protected readonly errorDeCarga = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly contar = contar;
  protected readonly grupoPorExcluir = signal<GrupoEmparejamiento | null>(null);

  // Formulario de creación (reutiliza FormularioAsignatura del módulo de asignaturas).
  protected readonly grupoEnCreacion = signal<GrupoEmparejamiento | null>(null);
  protected readonly datosNuevaAsignatura = signal<AsignaturaRequest>({
    nombre: '',
    servicioId: null,
  });
  protected readonly errorFormulario = signal('');

  protected readonly opcionesDeServicio = computed<OpcionCatalogo[]>(() =>
    this.serviciosDelSistema()
      .filter((servicio) => servicio.activo)
      .map((servicio) => ({ id: servicio.id, nombre: servicio.nombre }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre)),
  );
  protected readonly grupos = computed(() =>
    emparejarAsignaturas(
      this.filas(),
      indexarPorNumeroDeFila(this.filasOriginales()),
      this.serviciosDelSistema(),
      this.asignaturasDelSistema(),
    ),
  );
  protected readonly totalPendientes = computed(() => this.grupos().filter(estaPendiente).length);
  protected readonly totalNuevas = computed(
    () => this.grupos().filter((grupo) => grupo.estado === 'nuevo').length,
  );
  /** Nuevas sin parecido con ninguna asignatura de su servicio: se pueden crear sin revisar. */
  protected readonly gruposParaCrearEnLote = computed(() =>
    this.grupos().filter((grupo) => grupo.estado === 'nuevo' && !grupo.sugerencias.length),
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
      const [servicios, asignaturas] = await Promise.all([
        consultarTodasLasPaginas((pagina, tamano) =>
          this.servicioService.listar({ pagina, tamano }),
        ),
        consultarTodasLasPaginas((pagina, tamano) =>
          this.asignaturaService.listar({ pagina, tamano }),
        ),
      ]);
      this.serviciosDelSistema.set(servicios);
      this.asignaturasDelSistema.set(asignaturas);
    } catch (e) {
      this.errorDeCarga.set(mensajeError(e));
    } finally {
      this.cargandoCatalogos.set(false);
    }
  }

  // ---------- Crear ----------

  protected abrirCreacion(grupo: GrupoEmparejamiento) {
    this.datosNuevaAsignatura.set({ nombre: grupo.valorEnExcel, servicioId: grupo.contextoId });
    this.errorFormulario.set('');
    this.grupoEnCreacion.set(grupo);
  }

  protected cerrarCreacion() {
    if (!this.guardando()) this.grupoEnCreacion.set(null);
  }

  protected async guardarAsignaturaNueva() {
    const grupo = this.grupoEnCreacion();
    if (!grupo || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      const asignaturaCreada = await this.crearAsignatura(this.datosNuevaAsignatura());
      this.filasCambiadas.emit(
        this.aplicarAsignaturaEnFilas(this.filas(), grupo, asignaturaCreada),
      );
      this.grupoEnCreacion.set(null);
      this.mostrarAviso(
        `Asignatura «${asignaturaCreada.nombre}» creada y asignada a ${contar(grupo.numerosFila.length, 'fila', 'filas')}.`,
      );
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async crearAsignaturasEnLote() {
    const gruposACrear = this.gruposParaCrearEnLote();
    if (!gruposACrear.length || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    let filasActualizadas = this.filas();
    const { procesados, errores } = await procesarEnLote(
      gruposACrear,
      async (grupo) => {
        const asignaturaCreada = await this.crearAsignatura({
          nombre: grupo.valorEnExcel,
          servicioId: grupo.contextoId,
        });
        filasActualizadas = this.aplicarAsignaturaEnFilas(
          filasActualizadas,
          grupo,
          asignaturaCreada,
        );
      },
      (grupo) => `${grupo.valorEnExcel} (${grupo.contexto})`,
      mensajeError,
    );
    this.filasCambiadas.emit(filasActualizadas);
    this.guardando.set(false);
    this.mostrarAviso(
      `${segunCantidad(procesados, 'Se creó', 'Se crearon')} ${contar(procesados, 'asignatura', 'asignaturas')}.`,
    );
    if (errores.length) this.error.set('No se pudieron crear: ' + errores.join(' · '));
  }

  private async crearAsignatura(datos: AsignaturaRequest): Promise<Asignatura> {
    const asignaturaCreada = await firstValueFrom(
      this.asignaturaService.crear({ ...datos, nombre: datos.nombre.trim() }),
    );
    this.reemplazarAsignaturaEnCatalogo(asignaturaCreada);
    return asignaturaCreada;
  }

  // ---------- Asociar, activar ----------

  protected asociarAsignaturaExistente({ grupo, registroId }: AsociacionElegida) {
    const asignaturaElegida = this.asignaturasDelSistema().find(
      (asignatura) => asignatura.id === registroId,
    );
    if (!asignaturaElegida) return;
    this.filasCambiadas.emit(this.aplicarAsignaturaEnFilas(this.filas(), grupo, asignaturaElegida));
    this.mostrarAviso(
      `${contar(grupo.numerosFila.length, 'fila', 'filas')} ${segunCantidad(grupo.numerosFila.length, 'ahora usa', 'ahora usan')} «${asignaturaElegida.nombre}».`,
    );
  }

  protected async activarAsignatura(grupo: GrupoEmparejamiento) {
    if (!grupo.registro || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      const asignaturaActivada = await firstValueFrom(
        this.asignaturaService.cambiarEstado(grupo.registro.id, 'Activo'),
      );
      this.reemplazarAsignaturaEnCatalogo(asignaturaActivada);
      this.mostrarAviso(`Asignatura «${asignaturaActivada.nombre}» activada.`);
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

  /**
   * Las filas del grupo pasan a usar el nombre exacto de la asignatura. Si al crearla se
   * eligió otro servicio, las filas cambian también de servicio y toman sus cupos, para
   * que no queden cupos de un servicio distinto.
   */
  private aplicarAsignaturaEnFilas(
    filas: FilaCargaInicial[],
    grupo: GrupoEmparejamiento,
    asignatura: Asignatura,
  ): FilaCargaInicial[] {
    const cambiaDeServicio = !esMismoTexto(asignatura.servicio, grupo.contexto);
    const servicioNuevo = this.serviciosDelSistema().find(
      (servicio) => servicio.id === asignatura.servicioId,
    );
    return actualizarFilas(filas, grupo.numerosFila, {
      asignatura: asignatura.nombre,
      servicio: asignatura.servicio,
      ...(cambiaDeServicio && servicioNuevo
        ? {
            cupoMaximo: String(servicioNuevo.cupoMaximoInstalado),
            cupoSimultaneo: String(servicioNuevo.cupoSimultaneo),
          }
        : {}),
    });
  }

  private reemplazarAsignaturaEnCatalogo(asignaturaActualizada: Asignatura) {
    this.asignaturasDelSistema.update((asignaturas) => [
      ...asignaturas.filter((asignatura) => asignatura.id !== asignaturaActualizada.id),
      asignaturaActualizada,
    ]);
  }

  private mostrarAviso(texto: string) {
    this.error.set('');
    this.aviso.set(texto);
  }
}
