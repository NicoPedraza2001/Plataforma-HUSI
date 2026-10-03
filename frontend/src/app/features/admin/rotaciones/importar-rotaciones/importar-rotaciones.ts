import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { FilaCargaInicial, ResultadoCargaInicial } from '../../../../models/carga-inicial.model';
import { mensajeError } from '../../../../services/api-error';
import { CargaInicialService } from '../../../../services/carga-inicial.service';
import { descargarLibro, HojaExcel, leerExcel } from '../../../../shared/excel/excel';
import { RevisarCarga } from '../revisar-carga/revisar-carga';
import {
  CAMPOS_CARGA,
  detectarColumnas,
  prepararCarga,
  esPlantillaFinal,
  PLANTILLA_FINAL,
} from './campos-carga';

@Component({
  selector: 'app-importar-rotaciones',
  imports: [FormsModule, RevisarCarga],
  templateUrl: './importar-rotaciones.html',
  styleUrl: './importar-rotaciones.css',
})
export class ImportarRotaciones {
  private readonly api = inject(CargaInicialService);
  private readonly router = inject(Router);
  protected readonly campos = CAMPOS_CARGA;
  protected readonly pasos = [
    'Archivo',
    'Columnas',
    'Revisar y ordenar',
    'Validar',
    'Confirmar',
    'Resultado',
  ];
  protected readonly paso = signal(0);
  protected readonly ocupado = signal(false);
  protected readonly editando = signal(false);
  protected readonly error = signal('');
  protected readonly archivo = signal('');
  protected readonly hojas = signal<HojaExcel[]>([]);
  protected readonly filas = signal<FilaCargaInicial[]>([]);
  protected readonly resultado = signal<ResultadoCargaInicial | null>(null);
  protected readonly terminado = signal(false);
  protected readonly filtro = signal('Todos');
  protected readonly detallePagina = signal(1);
  protected hoja = 0;
  protected encabezado = 1;
  protected columnas = CAMPOS_CARGA.map(() => -1);
  protected comunes = CAMPOS_CARGA.map((c) => (c.clave === 'tipoDocumento' ? 'CC' : ''));
  protected crearFaltantes = true;
  private mapeoPendiente = true;
  protected readonly filtradas = computed(
    () =>
      this.resultado()?.filas.filter(
        (f) => this.filtro() === 'Todos' || f.estado === this.filtro(),
      ) ?? [],
  );
  protected readonly paginas = computed(() => Math.max(1, Math.ceil(this.filtradas().length / 20)));
  protected readonly detalle = computed(() =>
    this.filtradas().slice((this.detallePagina() - 1) * 20, this.detallePagina() * 20),
  );
  protected readonly resumen = computed(() => Object.entries(this.resultado()?.nuevos ?? {}));
  protected plantillaReconocida() {
    return esPlantillaFinal(this.encabezados());
  }
  protected encabezados() {
    return this.hojas()[this.hoja]?.filas[this.encabezado - 1] ?? [];
  }
  protected disponible(p: number) {
    if (this.ocupado() || this.editando()) return false;
    if (this.terminado()) return p === 5;
    return (
      p === 0 ||
      (p === 1 && !!this.archivo()) ||
      (p === 2 && this.filas().length > 0 && !this.mapeoPendiente) ||
      (p === 3 && !!this.resultado()) ||
      (p === 4 && !!this.resultado()?.validos)
    );
  }
  protected ir(p: number) {
    if (this.disponible(p)) {
      this.paso.set(p);
      this.error.set('');
    }
  }
  protected invalidar(mapeo = false) {
    this.resultado.set(null);
    this.detallePagina.set(1);
    this.filtro.set('Todos');
    if (mapeo) this.mapeoPendiente = true;
  }
  protected cambiarFilas(filas: FilaCargaInicial[]) {
    if (this.ocupado()) return;
    this.filas.set(filas);
    this.invalidar();
  }
  protected cambiarHoja() {
    this.encabezado = 1;
    this.detectar();
  }
  protected detectar() {
    this.columnas = detectarColumnas(this.encabezados());
    this.invalidar(true);
  }
  protected reiniciar() {
    if (this.ocupado()) return;
    this.hojas.set([]);
    this.archivo.set('');
    this.filas.set([]);
    this.error.set('');
    this.terminado.set(false);
    this.hoja = 0;
    this.encabezado = 1;
    this.columnas = this.campos.map(() => -1);
    this.comunes = this.campos.map((c) => (c.clave === 'tipoDocumento' ? 'CC' : ''));
    this.invalidar(true);
    this.paso.set(0);
  }
  protected async seleccionar(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) await this.cargar(input.files[0]);
    input.value = '';
  }
  protected async soltar(e: DragEvent) {
    e.preventDefault();
    if (e.dataTransfer?.files[0]) await this.cargar(e.dataTransfer.files[0]);
  }
  protected async cargar(file: File) {
    if (this.ocupado()) return;
    this.error.set('');
    this.ocupado.set(true);
    try {
      const hojas = await leerExcel(file, 5100);
      this.comunes = this.campos.map((c) => (c.clave === 'tipoDocumento' ? 'CC' : ''));
      this.hojas.set(hojas);
      this.archivo.set(file.name);
      this.hoja = 0;
      this.encabezado = 1;
      this.filas.set([]);
      this.terminado.set(false);
      this.detectar();
      this.paso.set(0);
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'No se pudo leer el Excel.');
    } finally {
      this.ocupado.set(false);
    }
  }
  protected preparar() {
    if (this.ocupado()) return;
    this.error.set('');
    const datos = this.hojas()[this.hoja]?.filas ?? [];
    const elegidas = this.columnas.filter((c) => c >= 0);
    if (
      !Number.isInteger(this.encabezado) ||
      this.encabezado < 1 ||
      this.encabezado > datos.length ||
      !elegidas.length ||
      new Set(elegidas).size !== elegidas.length ||
      elegidas.some((c) => c >= this.encabezados().length)
    ) {
      this.error.set(
        'Selecciona una fila de encabezados válida y columnas diferentes para cada campo.',
      );
      return;
    }
    if (this.mapeoPendiente) {
      const filas = prepararCarga(datos, this.encabezado, this.columnas, this.comunes);
      if (!filas.length || filas.length > 5000) {
        this.error.set('El archivo debe contener entre 1 y 5000 filas de datos.');
        return;
      }
      this.filas.set(filas);
      this.invalidar();
      this.mapeoPendiente = false;
    }
    this.paso.set(2);
  }
  protected async validar() {
    if (this.ocupado() || this.editando() || !this.filas().length || this.terminado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.invalidar();
    try {
      this.resultado.set(await firstValueFrom(this.api.validar(this.filas(), this.crearFaltantes)));
      this.paso.set(3);
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.ocupado.set(false);
    }
  }
  protected async importar() {
    const r = this.resultado();
    if (this.ocupado() || !r?.validos || this.terminado() || this.paso() !== 4) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      this.resultado.set(
        await firstValueFrom(this.api.confirmar(this.filas(), this.crearFaltantes, r.validacion)),
      );
      this.terminado.set(true);
      this.paso.set(5);
      this.detallePagina.set(1);
      this.filtro.set('Todos');
    } catch (e) {
      this.invalidar();
      this.paso.set(2);
      this.error.set(
        mensajeError(e) +
          ' Vuelve a validar antes de reintentar. Las relaciones existentes se reconocerán sin duplicarlas.',
      );
    } finally {
      this.ocupado.set(false);
    }
  }
  protected async descargar(reporte = false) {
    try {
      const resultado = this.resultado();
      const filas = reporte
        ? this.filas().map((f) => {
            const d = resultado?.filas.find((d) => d.fila === f.fila);
            return [
              ...this.campos.map((c) => f[c.clave]),
              f.fila,
              d?.estado ?? '',
              d?.mensaje ?? '',
            ];
          })
        : [];
      await descargarLibro(
        reporte ? 'resultado-carga-inicial.xlsx' : 'plantilla-carga-inicial.xlsx',
        'Rotaciones',
        [
          ...(reporte ? this.campos.map((c) => c.nombre) : PLANTILLA_FINAL),
          ...(reporte ? ['Fila original', 'Resultado', 'Detalle'] : []),
        ],
        filas,
      );
    } catch {
      this.error.set('No se pudo descargar el archivo.');
    }
  }

  /** Sale del asistente hacia el listado de rotaciones. */
  protected volver() {
    this.router.navigate(['/admin/rotaciones']);
  }
}
