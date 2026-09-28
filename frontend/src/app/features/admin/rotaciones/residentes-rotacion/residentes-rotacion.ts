import { Component, inject, input, OnInit, output, signal } from '@angular/core';
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

@Component({
  selector: 'app-residentes-rotacion',
  imports: [FormsModule, FocoModal, Paginacion],
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
  protected readonly candidatos = new Consulta<OpcionCatalogo[]>((p) =>
    this.api.candidatos(this.rotacion(), String(p['q'] ?? '')),
  );
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected termino = '';
  protected seleccionado = 0;

  ngOnInit() {
    this.cargar();
    this.buscar();
  }

  protected cargar(pagina = 1) {
    this.consulta.cargar({ pagina });
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
      estado === 'Cancelado' ? 'Asignación cancelada; se conserva el historial.' : 'Cupo asignado.',
    );
  }

  private async ejecutar(accion: () => Promise<void>, mensaje: string) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    this.aviso.set('');
    try {
      await accion();
      this.aviso.set(mensaje);
      this.cargar(this.consulta.datos()?.pagina ?? 1);
      this.buscar();
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
