import { Component, input, output, signal } from '@angular/core';
import { descargarLibro, leerExcel } from '../../../../../shared/excel/excel';
import { LibroExcel, MAXIMO_FILAS_CARGA, PLANTILLA_FINAL } from '../campos-carga';

/** Paso 1: el administrador elige el Excel; se lee en el navegador sin enviarlo al backend. */
@Component({
  selector: 'app-paso-archivo',
  imports: [],
  templateUrl: './paso-archivo.html',
  styleUrl: './paso-archivo.css',
})
export class PasoArchivo {
  readonly libroExcel = input<LibroExcel | null>(null);
  readonly archivoLeido = output<LibroExcel>();
  readonly archivoQuitado = output<void>();
  readonly siguiente = output<void>();

  protected readonly leyendoArchivo = signal(false);
  protected readonly error = signal('');
  protected readonly maximoFilas = MAXIMO_FILAS_CARGA;

  protected async seleccionarArchivo(evento: Event) {
    const campoArchivo = evento.target as HTMLInputElement;
    const archivo = campoArchivo.files?.[0];
    if (archivo) await this.leerArchivo(archivo);
    campoArchivo.value = '';
  }

  protected async soltarArchivo(evento: DragEvent) {
    evento.preventDefault();
    const archivo = evento.dataTransfer?.files[0];
    if (archivo) await this.leerArchivo(archivo);
  }

  private async leerArchivo(archivo: File) {
    if (this.leyendoArchivo()) return;
    this.error.set('');
    this.leyendoArchivo.set(true);
    try {
      const hojas = await leerExcel(archivo, MAXIMO_FILAS_CARGA + 100);
      this.archivoLeido.emit({ nombreArchivo: archivo.name, hojas });
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'No se pudo leer el Excel.');
    } finally {
      this.leyendoArchivo.set(false);
    }
  }

  protected async descargarPlantilla() {
    try {
      await descargarLibro('plantilla-carga-inicial.xlsx', 'Rotaciones', PLANTILLA_FINAL, []);
    } catch {
      this.error.set('No se pudo descargar la plantilla.');
    }
  }
}
