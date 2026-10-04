import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { Pagina } from '../../../../models/pagina.model';
import { ResidenteRotacion, RotacionGestion } from '../../../../models/rotacion.model';
import { mensajeError } from '../../../../services/api-error';
import { RotacionService } from '../../../../services/rotacion.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { EsperaServicio } from './espera-servicio/espera-servicio';

@Component({
  selector: 'app-residentes-rotacion',
  imports: [FormsModule, FocoModal, Paginacion, EsperaServicio, DatePipe],
  templateUrl: './residentes-rotacion.html',
  styleUrl: './residentes-rotacion.css',
})
export class ResidentesRotacion implements OnInit {
  // Dependencias
  private readonly api = inject(RotacionService);

  // Estado de la pantalla y datos del formulario
  readonly rotacion = input.required<RotacionGestion>();
  readonly cerrar = output<void>();
  protected readonly consulta = new Consulta<Pagina<ResidenteRotacion>>((p) =>
    this.api.residentes(this.rotacion(), Number(p['pagina'] ?? 1)),
  );
  /** Residentes en «Esperando cupo», en orden de llegada. */
  protected readonly enEspera = new Consulta<Pagina<ResidenteRotacion>>((p) =>
    this.api.residentes(this.rotacion(), Number(p['pagina'] ?? 1), true),
  );
  protected readonly candidatos = new Consulta<OpcionCatalogo[]>((p) =>
    this.api.candidatos(this.rotacion(), String(p['q'] ?? '')),
  );
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly porEliminar = signal<ResidenteRotacion | null>(null);
  private readonly espera = viewChild(EsperaServicio);
  protected termino = '';
  protected seleccionado = 0;

  ngOnInit() {
    this.cargar();
    this.cargarEspera();
    this.buscar();
  }

  protected cargar(pagina = 1) {
    this.consulta.cargar({ pagina });
  }

  protected cargarEspera(pagina = 1) {
    this.enEspera.cargar({ pagina });
  }

  /** Posición en la lista de espera (1 = el siguiente en recibir cupo). */
  protected posicion(indice: number) {
    const datos = this.enEspera.datos();
    return ((datos?.pagina ?? 1) - 1) * (datos?.tamano ?? 10) + indice + 1;
  }

  protected buscar() {
    this.seleccionado = 0;
    this.candidatos.cargar({ q: this.termino }, 300);
  }

  protected async asignar() {
    if (!this.seleccionado || this.guardando()) return;
    await this.ejecutar(
      () => firstValueFrom(this.api.asignar(this.rotacion(), Number(this.seleccionado))),
      'Residente registrado. Si no había cupos, quedó en espera.',
    );
  }

  protected async estado(fila: ResidenteRotacion, estado: string) {
    await this.ejecutar(
      () => firstValueFrom(this.api.estadoResidente(this.rotacion(), fila.residenteId, estado)),
      estado === 'Esperando cupo'
        ? `Se quitó el cupo a ${fila.nombre}: pasó al final de la lista de espera. Si alguien esperaba, el primero de la lista ocupó su cupo.`
        : 'Cupo asignado.',
    );
  }

  /** Pide confirmación antes de borrar la relación de un residente sin cupo. */
  protected solicitarEliminar(fila: ResidenteRotacion) {
    this.error.set('');
    this.porEliminar.set(fila);
  }

  protected async eliminar() {
    const fila = this.porEliminar();
    if (!fila) return;
    const eliminado = await this.ejecutar(
      () => firstValueFrom(this.api.eliminarResidente(this.rotacion(), fila.residenteId)),
      `${fila.nombre} se eliminó de esta rotación.`,
    );
    if (eliminado) this.porEliminar.set(null);
  }

  protected trasladados(cantidad: number) {
    this.aviso.set(
      cantidad === 1
        ? '1 residente pasó de la lista de espera a esta rotación.'
        : `${cantidad} residentes pasaron de la lista de espera a esta rotación.`,
    );
    this.error.set('');
    this.refrescar();
  }

  private refrescar() {
    this.cargar(this.consulta.datos()?.pagina ?? 1);
    this.cargarEspera(this.enEspera.datos()?.pagina ?? 1);
    this.buscar();
    this.espera()?.cargar();
  }

  /** Devuelve si la acción se completó. */
  private async ejecutar(accion: () => Promise<void>, mensaje: string): Promise<boolean> {
    if (this.guardando()) return false;
    this.guardando.set(true);
    this.error.set('');
    this.aviso.set('');
    try {
      await accion();
      this.aviso.set(mensaje);
      this.refrescar();
      return true;
    } catch (e) {
      this.error.set(mensajeError(e));
      return false;
    } finally {
      this.guardando.set(false);
    }
  }
}
