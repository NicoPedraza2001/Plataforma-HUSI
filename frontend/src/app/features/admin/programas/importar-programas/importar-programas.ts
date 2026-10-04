import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import {
  FilaImportacion,
  ResultadoImportacion,
} from '../../../../models/importacion-programas.model';
import { Universidad } from '../../../../models/universidad.model';
import { mensajeError } from '../../../../services/api-error';
import { ProgramaService } from '../../../../services/programa.service';
import {
  HojaExcel,
  MARCA_FORMULA,
  descargarLibro,
  leerExcel,
  letraColumna,
} from '../../../../shared/excel/excel';
import { mapearFilas } from './mapear-filas';

@Component({
  selector: 'app-importar-programas',
  imports: [FormsModule],
  templateUrl: './importar-programas.html',
  styleUrl: './importar-programas.css',
})
export class ImportarProgramas {
  // Dependencias
  private readonly programaService = inject(ProgramaService);

  // Estado de la pantalla y datos del formulario
  readonly universidades = input.required<Universidad[]>();
  readonly cerrar = output<void>();
  protected readonly paso = signal(1);
  protected readonly ocupado = signal(false);
  protected readonly error = signal('');
  protected readonly hojas = signal<HojaExcel[]>([]);
  protected readonly nombreArchivo = signal('');
  protected readonly indiceHoja = signal(0);
  protected readonly filaEncabezado = signal(1);
  protected readonly columnaNombre = signal(-1);
  protected readonly columnaUniversidad = signal(-1);
  protected readonly resultado = signal<ResultadoImportacion | null>(null);
  protected readonly filtro = signal('TODOS');
  protected readonly paginaDetalle = signal(1);
  private filasValidadas: FilaImportacion[] = [];
  protected readonly hoja = computed<HojaExcel | undefined>(() =>
    this.hojas().at(this.indiceHoja()),
  );
  protected readonly maxFilas = computed(() => this.hoja()?.filas.length ?? 1);
  protected readonly columnas = computed(() => {
    const fila = this.hoja()?.filas[this.filaEncabezado() - 1] ?? [];
    return fila.map((nombre, indice) => ({
      indice,
      etiqueta: letraColumna(indice) + ' · ' + (nombre || 'Sin encabezado'),
    }));
  });
  protected readonly mapeoValido = computed(
    () =>
      Number.isInteger(this.filaEncabezado()) &&
      this.filaEncabezado() >= 1 &&
      this.filaEncabezado() <= (this.hoja()?.filas.length ?? 0) &&
      this.columnaNombre() >= 0 &&
      this.columnaUniversidad() >= 0 &&
      this.columnaNombre() !== this.columnaUniversidad() &&
      this.columnaNombre() < this.columnas().length &&
      this.columnaUniversidad() < this.columnas().length,
  );
  protected readonly filas = computed(() =>
    this.mapeoValido()
      ? mapearFilas(
          this.hoja()!,
          this.filaEncabezado(),
          this.columnaNombre(),
          this.columnaUniversidad(),
        )
      : [],
  );
  protected readonly detalleFiltrado = computed(() =>
    (this.resultado()?.filas ?? []).filter(
      (fila) => this.filtro() === 'TODOS' || fila.estado === this.filtro(),
    ),
  );
  protected readonly paginasDetalle = computed(() =>
    Math.max(1, Math.ceil(this.detalleFiltrado().length / 20)),
  );
  protected readonly detallePagina = computed(() =>
    this.detalleFiltrado().slice((this.paginaDetalle() - 1) * 20, this.paginaDetalle() * 20),
  );

  protected quitarArchivo(): void {
    if (this.ocupado()) return;
    this.hojas.set([]);
    this.nombreArchivo.set('');
    this.indiceHoja.set(0);
    this.filaEncabezado.set(1);
    this.columnaNombre.set(-1);
    this.columnaUniversidad.set(-1);
    this.resultado.set(null);
    this.filasValidadas = [];
    this.error.set('');
    this.filtro.set('TODOS');
    this.paginaDetalle.set(1);
    this.paso.set(1);
  }

  protected async seleccionarArchivo(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) await this.leerArchivo(input.files[0]);
    input.value = '';
  }

  protected async soltarArchivo(event: DragEvent) {
    event.preventDefault();
    if (!this.ocupado() && event.dataTransfer?.files[0])
      await this.leerArchivo(event.dataTransfer.files[0]);
  }

  protected async leerArchivo(archivo: File) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.hojas.set([]);
    this.nombreArchivo.set('');
    this.resultado.set(null);
    try {
      this.hojas.set(await leerExcel(archivo));
      this.nombreArchivo.set(archivo.name);
      this.indiceHoja.set(0);
      this.filaEncabezado.set(1);
      this.sugerirColumnas();
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'No se pudo leer el archivo.');
    } finally {
      this.ocupado.set(false);
    }
  }

  protected cambiarHoja(indice: number) {
    this.indiceHoja.set(Number(indice));
    this.filaEncabezado.set(1);
    this.sugerirColumnas();
  }

  protected cambiarEncabezado(fila: number) {
    this.filaEncabezado.set(Number(fila));
    this.sugerirColumnas();
  }

  protected sugerirColumnas() {
    const normalizar = (texto: string) =>
      texto
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/[_\s]+/g, ' ')
        .trim();
    const nombres = (this.hoja()?.filas[this.filaEncabezado() - 1] ?? []).map(normalizar);
    this.columnaNombre.set(
      nombres.findIndex((encabezado) =>
        [
          'nombre',
          'programa',
          'nombre programa',
          'nombre del programa',
          'nombre academico',
        ].includes(encabezado),
      ),
    );
    this.columnaUniversidad.set(
      nombres.findIndex((encabezado) =>
        ['universidad', 'nombre universidad', 'nombre de la universidad', 'institucion'].includes(
          encabezado,
        ),
      ),
    );
    this.resultado.set(null);
    this.error.set('');
  }

  protected async validar() {
    if (this.ocupado() || !this.mapeoValido()) return;
    this.error.set('');
    if (!this.filas().length || this.filas().length > 1000) {
      this.error.set(
        'Selecciona entre 1 y 1000 filas de datos. Las filas completamente vacías se ignoran.',
      );
      return;
    }
    if (
      this.filas().some(
        (fila) => fila.nombre.includes(MARCA_FORMULA) || fila.universidad.includes(MARCA_FORMULA),
      )
    ) {
      this.error.set(
        'Hay fórmulas en las columnas seleccionadas. En Excel, copia esas columnas y usa Pegar valores antes de importar.',
      );
      return;
    }
    this.ocupado.set(true);
    try {
      this.filasValidadas = this.filas().map((fila) => ({ ...fila }));
      this.resultado.set(await firstValueFrom(this.programaService.validar(this.filasValidadas)));
      this.paso.set(3);
      this.filtrarDetalle('TODOS');
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.ocupado.set(false);
    }
  }

  protected async importar() {
    if (this.ocupado() || !this.resultado()?.validos || this.paso() !== 3) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      this.resultado.set(await firstValueFrom(this.programaService.importar(this.filasValidadas)));
      this.paso.set(4);
      this.filtrarDetalle('TODOS');
    } catch (e) {
      this.error.set(
        mensajeError(e) +
          ' Si se perdió la conexión durante el guardado, vuelve a validar: los programas ya guardados se omitirán.',
      );
    } finally {
      this.ocupado.set(false);
    }
  }

  protected filtrarDetalle(estado: string) {
    this.filtro.set(estado);
    this.paginaDetalle.set(1);
  }

  protected async descargarPlantilla() {
    this.error.set('');
    try {
      await descargarLibro(
        'plantilla-programas.xlsx',
        'Programas',
        ['Nombre del programa', 'Universidad'],
        [
          [
            'Programa de ejemplo — reemplazar',
            this.universidades()[0]?.nombre ?? 'Nombre exacto de una universidad activa',
          ],
        ],
      );
    } catch {
      this.error.set('No se pudo descargar la plantilla. Intenta de nuevo.');
    }
  }

  protected async descargarErrores() {
    this.error.set('');
    try {
      await descargarLibro(
        'resultado-importacion-programas.xlsx',
        'Programas',
        ['Fila', 'Programa', 'Universidad', 'Estado', 'Mensaje'],
        (this.resultado()?.filas ?? [])
          .filter((fila) => ['ERROR', 'OMITIDO'].includes(fila.estado))
          .map((fila) => [
            fila.fila,
            fila.nombre ?? '',
            fila.universidad ?? '',
            fila.estado,
            fila.mensaje,
          ]),
      );
    } catch {
      this.error.set('No se pudo descargar el informe. Intenta de nuevo.');
    }
  }
}
