import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FilaCargaInicial, ResultadoCargaInicial } from '../../../../models/carga-inicial.model';
import { LoteImportacion } from '../../../../services/lote-importacion';
import {
  ConfiguracionColumnas,
  crearConfiguracionInicial,
  LibroExcel,
  sonConfiguracionesIguales,
} from './campos-carga';
import { PasoArchivo } from './paso-archivo/paso-archivo';
import { PasoAsignaturas } from './paso-asignaturas/paso-asignaturas';
import { ColumnasConfirmadas, PasoColumnas } from './paso-columnas/paso-columnas';
import { PasoConfirmacion } from './paso-confirmacion/paso-confirmacion';
import { PasoProgramas } from './paso-programas/paso-programas';
import { PasoResidentes } from './paso-residentes/paso-residentes';
import { PasoRotaciones } from './paso-rotaciones/paso-rotaciones';
import { PasoServicios } from './paso-servicios/paso-servicios';
import { PasoUniversidades } from './paso-universidades/paso-universidades';
import { hayQueGuardar, IdPasoImportacion, PASOS_IMPORTACION } from './pasos-importacion';

type PendientesPorPaso = Partial<Record<IdPasoImportacion, number>>;

/**
 * Asistente de importación de la carga inicial desde Excel. Este componente solo coordina:
 * guarda las filas (única fuente de verdad), decide qué pasos están disponibles y los
 * conecta. Cada paso vive en su carpeta y resuelve su parte del diagrama entidad-relación.
 */
@Component({
  selector: 'app-importar-rotaciones',
  imports: [
    PasoArchivo,
    PasoColumnas,
    PasoUniversidades,
    PasoProgramas,
    PasoServicios,
    PasoAsignaturas,
    PasoResidentes,
    PasoRotaciones,
    PasoConfirmacion,
  ],
  templateUrl: './importar-rotaciones.html',
  styleUrl: './importar-rotaciones.css',
})
export class ImportarRotaciones {
  private readonly router = inject(Router);
  /**
   * Todo lo que se crea o modifica en el asistente queda como una sola fila del historial.
   * Salir sin confirmar (botón «Cancelar importación», otra pantalla, otro archivo) la cancela.
   */
  private readonly loteImportacion = inject(LoteImportacion);

  protected readonly pasos = PASOS_IMPORTACION;
  protected readonly pasoActual = signal<IdPasoImportacion>('archivo');

  // Datos del archivo
  protected readonly libroExcel = signal<LibroExcel | null>(null);
  protected readonly configuracionColumnas = signal<ConfiguracionColumnas | null>(null);
  /** Filas tal como se leyeron del Excel; sirven para mostrar qué se corrigió. */
  protected readonly filasOriginales = signal<FilaCargaInicial[]>([]);
  /** Filas con las correcciones de cada paso: es lo que se valida y se guarda. */
  protected readonly filas = signal<FilaCargaInicial[]>([]);

  // Progreso
  protected readonly pendientesPorPaso = signal<PendientesPorPaso>({});
  protected readonly resultadoValidacion = signal<ResultadoCargaInicial | null>(null);
  protected readonly resultadoFinal = signal<ResultadoCargaInicial | null>(null);
  /** Un paso está guardando, validando o editando: se bloquea la barra de pasos. */
  protected readonly pasoTrabajando = signal(false);
  protected readonly error = signal('');

  protected readonly hayCorrecciones = computed(() => this.filas() !== this.filasOriginales());
  /** Filas que el administrador excluyó en algún paso; no llegan al backend. */
  protected readonly filasExcluidas = computed(() => {
    const enviadas = new Set(this.filas().map((fila) => fila.fila));
    return this.filasOriginales().filter((fila) => !enviadas.has(fila.fila));
  });
  protected readonly numeroPasoActual = computed(
    () => this.pasos.findIndex((paso) => paso.id === this.pasoActual()) + 1,
  );

  constructor() {
    inject(DestroyRef).onDestroy(() => this.loteImportacion.cancelar());
  }

  // ---------- Navegación ----------

  protected estaDisponible(idPaso: IdPasoImportacion): boolean {
    if (this.pasoTrabajando()) return false;
    if (this.resultadoFinal()) return idPaso === 'confirmacion';
    const pendientes = this.pendientesPorPaso();
    const pasoResuelto = (paso: IdPasoImportacion) => pendientes[paso] === 0;
    switch (idPaso) {
      case 'archivo':
        return true;
      case 'columnas':
        return !!this.libroExcel();
      case 'universidades':
        return this.filas().length > 0;
      case 'programas':
        return this.filas().length > 0 && pasoResuelto('universidades');
      case 'servicios':
        return this.estaDisponible('programas') && pasoResuelto('programas');
      case 'asignaturas':
        return this.estaDisponible('servicios') && pasoResuelto('servicios');
      case 'residentes':
        return this.estaDisponible('asignaturas') && pasoResuelto('asignaturas');
      case 'rotaciones':
        return this.estaDisponible('residentes') && pasoResuelto('residentes');
      case 'confirmacion':
        return this.estaDisponible('rotaciones') && hayQueGuardar(this.resultadoValidacion());
    }
  }

  protected irAPaso(idPaso: IdPasoImportacion) {
    if (!this.estaDisponible(idPaso)) return;
    this.error.set('');
    this.pasoActual.set(idPaso);
  }

  protected irAlPasoSiguiente() {
    const siguiente = this.pasos[this.numeroPasoActual()];
    if (siguiente) this.irAPaso(siguiente.id);
  }

  protected irAlPasoAnterior() {
    const anterior = this.pasos[this.numeroPasoActual() - 2];
    if (anterior) this.irAPaso(anterior.id);
  }

  protected pendientesDelPaso(idPaso: IdPasoImportacion): number | undefined {
    return this.pendientesPorPaso()[idPaso];
  }

  // ---------- Eventos de los pasos ----------

  protected registrarArchivo(libro: LibroExcel) {
    this.reiniciarImportacion();
    this.loteImportacion.iniciar(libro.nombreArchivo);
    this.libroExcel.set(libro);
    this.configuracionColumnas.set(crearConfiguracionInicial(libro.hojas));
  }

  protected quitarArchivo() {
    this.reiniciarImportacion();
  }

  /**
   * Con la misma configuración se conservan las correcciones hechas en los pasos
   * siguientes; si cambió (o se pidió reconstruir) las filas se vuelven a leer del Excel.
   */
  protected aplicarColumnas({ configuracion, filas, reconstruir }: ColumnasConfirmadas) {
    const configuracionSinCambios = sonConfiguracionesIguales(
      configuracion,
      this.configuracionColumnas(),
    );
    if (reconstruir || !configuracionSinCambios || !this.filas().length) {
      this.configuracionColumnas.set(configuracion);
      this.filasOriginales.set(filas);
      this.filas.set(filas);
      this.pendientesPorPaso.set({});
      this.resultadoValidacion.set(null);
    }
    this.irAPaso('universidades');
  }

  /** Cualquier cambio en las filas invalida la validación anterior. */
  protected actualizarFilas(filasCorregidas: FilaCargaInicial[]) {
    this.filas.set(filasCorregidas);
    this.resultadoValidacion.set(null);
  }

  protected registrarPendientes(idPaso: IdPasoImportacion, cantidad: number) {
    this.pendientesPorPaso.update((pendientes) => ({ ...pendientes, [idPaso]: cantidad }));
  }

  protected registrarValidacion(resultado: ResultadoCargaInicial) {
    this.resultadoValidacion.set(resultado);
  }

  protected registrarCargaFinal(resultado: ResultadoCargaInicial) {
    this.resultadoFinal.set(resultado);
    this.loteImportacion.finalizar();
  }

  protected registrarFalloDeConfirmacion(mensaje: string) {
    this.resultadoValidacion.set(null);
    this.pasoActual.set('rotaciones');
    this.error.set(mensaje);
  }

  protected volverAlListado() {
    this.router.navigate(['/admin/rotaciones']);
  }

  private reiniciarImportacion() {
    this.loteImportacion.cancelar();
    this.libroExcel.set(null);
    this.configuracionColumnas.set(null);
    this.filasOriginales.set([]);
    this.filas.set([]);
    this.pendientesPorPaso.set({});
    this.resultadoValidacion.set(null);
    this.resultadoFinal.set(null);
    this.error.set('');
    this.pasoActual.set('archivo');
  }
}
