import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { Servicio, ServicioRequest } from '../../../../../models/servicio.model';
import { mensajeError } from '../../../../../services/api-error';
import { ServicioService } from '../../../../../services/servicio.service';
import { consultarTodasLasPaginas } from '../../../../../shared/consulta/todas-las-paginas';
import { FocoModal } from '../../../../../shared/foco-modal/foco-modal';
import { FormularioServicio } from '../../../servicios/formulario-servicio/formulario-servicio';
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
import { CuposEnExcel, emparejarServicios, leerCuposDelGrupo } from './emparejar-servicios';

/** Servicio nuevo que se puede crear sin revisar: sin parecidos y con cupos válidos. */
interface ServicioParaCrearEnLote {
  grupo: GrupoEmparejamiento;
  cupos: CuposEnExcel;
}

/**
 * Paso 5: servicios del Excel frente a los registrados. Permite crear los nuevos (con sus
 * cupos), asociar los mal escritos a uno existente, usar los cupos del sistema cuando no
 * coinciden, activar inactivos o excluir sus filas.
 */
@Component({
  selector: 'app-paso-servicios',
  imports: [TablaEmparejamiento, FormularioServicio, FocoModal, ConfirmarExclusion],
  templateUrl: './paso-servicios.html',
  styleUrl: './paso-servicios.css',
})
export class PasoServicios {
  private readonly servicioService = inject(ServicioService);

  readonly filas = input.required<FilaCargaInicial[]>();
  readonly filasOriginales = input.required<FilaCargaInicial[]>();
  readonly filasCambiadas = output<FilaCargaInicial[]>();
  readonly pendientesCambiados = output<number>();
  readonly trabajandoCambiado = output<boolean>();
  readonly anterior = output<void>();
  readonly siguiente = output<void>();

  protected readonly serviciosDelSistema = signal<Servicio[]>([]);
  protected readonly cargandoServicios = signal(true);
  protected readonly errorDeCarga = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly cantidadConPalabra = cantidadConPalabra;
  protected readonly grupoPorExcluir = signal<GrupoEmparejamiento | null>(null);

  // Formulario de creación (reutiliza FormularioServicio del módulo de servicios).
  protected readonly grupoEnCreacion = signal<GrupoEmparejamiento | null>(null);
  protected readonly datosNuevoServicio = signal<ServicioRequest>(this.crearDatosVacios());
  protected readonly errorFormulario = signal('');

  protected readonly grupos = computed(() =>
    emparejarServicios(
      this.filas(),
      indexarPorNumeroDeFila(this.filasOriginales()),
      this.serviciosDelSistema(),
    ),
  );
  protected readonly totalPendientes = computed(() => this.grupos().filter(estaPendiente).length);
  protected readonly totalNuevos = computed(
    () => this.grupos().filter((grupo) => grupo.estado === 'nuevo').length,
  );
  /** Filas de cada grupo, para leer sus cupos al crear el servicio. */
  private readonly filasPorGrupo = computed(() => {
    const filasPorNumero = indexarPorNumeroDeFila(this.filas());
    return new Map(
      this.grupos().map((grupo) => [
        grupo.clave,
        grupo.numerosFila.map((numeroFila) => filasPorNumero.get(numeroFila)!),
      ]),
    );
  });
  protected readonly serviciosParaCrearEnLote = computed<ServicioParaCrearEnLote[]>(() =>
    this.grupos()
      .filter((grupo) => grupo.estado === 'nuevo' && !grupo.sugerencias.length)
      .map((grupo) => ({ grupo, cupos: leerCuposDelGrupo(this.filasPorGrupo().get(grupo.clave)!) }))
      .filter((candidato): candidato is ServicioParaCrearEnLote => candidato.cupos !== null),
  );

  constructor() {
    effect(() => {
      if (!this.cargandoServicios() && !this.errorDeCarga()) {
        this.pendientesCambiados.emit(this.totalPendientes());
      }
    });
    effect(() => this.trabajandoCambiado.emit(this.guardando()));
    this.cargarServicios();
  }

  protected async cargarServicios() {
    this.cargandoServicios.set(true);
    this.errorDeCarga.set('');
    try {
      const servicios = await consultarTodasLasPaginas((pagina, tamano) =>
        this.servicioService.listar({ pagina, tamano }),
      );
      this.serviciosDelSistema.set(servicios);
    } catch (e) {
      this.errorDeCarga.set(mensajeError(e));
    } finally {
      this.cargandoServicios.set(false);
    }
  }

  // ---------- Crear ----------

  protected abrirCreacion(grupo: GrupoEmparejamiento) {
    const cupos = leerCuposDelGrupo(this.filasPorGrupo().get(grupo.clave) ?? []);
    this.datosNuevoServicio.set({
      nombre: grupo.valorEnExcel,
      descripcion: null,
      cupoMaximoInstalado: cupos?.cupoMaximoInstalado ?? null,
      cupoSimultaneo: cupos?.cupoSimultaneo ?? null,
    });
    this.errorFormulario.set('');
    this.grupoEnCreacion.set(grupo);
  }

  protected cerrarCreacion() {
    if (!this.guardando()) this.grupoEnCreacion.set(null);
  }

  protected async guardarServicioNuevo() {
    const grupo = this.grupoEnCreacion();
    if (!grupo || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      const servicioCreado = await this.crearServicio(this.datosNuevoServicio());
      this.filasCambiadas.emit(this.aplicarServicioEnFilas(this.filas(), grupo, servicioCreado));
      this.grupoEnCreacion.set(null);
      this.mostrarAviso(
        `Servicio «${servicioCreado.nombre}» creado y asignado a ${cantidadConPalabra(grupo.numerosFila.length, 'fila', 'filas')}.`,
      );
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async crearServiciosEnLote() {
    const serviciosACrear = this.serviciosParaCrearEnLote();
    if (!serviciosACrear.length || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    let filasActualizadas = this.filas();
    const { procesados, errores } = await procesarEnLote(
      serviciosACrear,
      async ({ grupo, cupos }) => {
        const servicioCreado = await this.crearServicio({
          nombre: grupo.valorEnExcel,
          descripcion: null,
          ...cupos,
        });
        filasActualizadas = this.aplicarServicioEnFilas(filasActualizadas, grupo, servicioCreado);
      },
      ({ grupo }) => grupo.valorEnExcel,
      mensajeError,
    );
    this.filasCambiadas.emit(filasActualizadas);
    this.guardando.set(false);
    this.mostrarAviso(
      `${segunCantidad(procesados, 'Se creó', 'Se crearon')} ${cantidadConPalabra(procesados, 'servicio', 'servicios')}.`,
    );
    if (errores.length) this.error.set('No se pudieron crear: ' + errores.join(' · '));
  }

  private async crearServicio(datos: ServicioRequest): Promise<Servicio> {
    const servicioCreado = await firstValueFrom(
      this.servicioService.crear({
        ...datos,
        nombre: datos.nombre.trim(),
        descripcion: datos.descripcion?.trim() || null,
      }),
    );
    this.reemplazarServicioEnCatalogo(servicioCreado);
    return servicioCreado;
  }

  // ---------- Asociar, usar datos del sistema, activar ----------

  protected asociarServicioExistente({ grupo, registroId }: AsociacionElegida) {
    const servicioElegido = this.serviciosDelSistema().find(
      (servicio) => servicio.id === registroId,
    );
    if (!servicioElegido) return;
    this.filasCambiadas.emit(this.aplicarServicioEnFilas(this.filas(), grupo, servicioElegido));
    this.mostrarAviso(
      `${cantidadConPalabra(grupo.numerosFila.length, 'fila', 'filas')} ${segunCantidad(grupo.numerosFila.length, 'ahora usa', 'ahora usan')} «${servicioElegido.nombre}» y sus cupos.`,
    );
  }

  protected usarDatosDelSistema(grupo: GrupoEmparejamiento) {
    if (grupo.registro) this.asociarServicioExistente({ grupo, registroId: grupo.registro.id });
  }

  protected async activarServicio(grupo: GrupoEmparejamiento) {
    if (!grupo.registro || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      const servicioActivado = await firstValueFrom(
        this.servicioService.cambiarEstado(grupo.registro.id, 'Activo'),
      );
      this.reemplazarServicioEnCatalogo(servicioActivado);
      this.mostrarAviso(`Servicio «${servicioActivado.nombre}» activado.`);
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

  /** Las filas del grupo pasan a usar el nombre exacto del servicio y sus cupos registrados. */
  private aplicarServicioEnFilas(
    filas: FilaCargaInicial[],
    grupo: GrupoEmparejamiento,
    servicio: Servicio,
  ): FilaCargaInicial[] {
    return actualizarFilas(filas, grupo.numerosFila, {
      servicio: servicio.nombre,
      cupoMaximo: String(servicio.cupoMaximoInstalado),
      cupoSimultaneo: String(servicio.cupoSimultaneo),
    });
  }

  private reemplazarServicioEnCatalogo(servicioActualizado: Servicio) {
    this.serviciosDelSistema.update((servicios) => [
      ...servicios.filter((servicio) => servicio.id !== servicioActualizado.id),
      servicioActualizado,
    ]);
  }

  private crearDatosVacios(): ServicioRequest {
    return { nombre: '', descripcion: null, cupoMaximoInstalado: null, cupoSimultaneo: null };
  }

  private mostrarAviso(texto: string) {
    this.error.set('');
    this.aviso.set(texto);
  }
}
