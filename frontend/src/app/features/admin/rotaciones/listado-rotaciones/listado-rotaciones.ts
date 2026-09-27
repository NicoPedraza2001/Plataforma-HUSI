import { ResidentesRotacion } from '../residentes-rotacion/residentes-rotacion';
import { FormularioRotacion } from '../formulario-rotacion/formulario-rotacion';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { RotacionService } from '../../../../services/rotacion.service';
import { RotacionGestion, CatalogosRotacion } from '../../../../models/rotacion.model';
import { Consulta } from '../../../../shared/consulta/consulta';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { mensajeError } from '../../../../services/api-error';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { ImportarRotaciones } from '../importar-rotaciones/importar-rotaciones';
@Component({
  selector: 'app-listado-rotaciones',
  imports: [
    FormularioRotacion,
    ResidentesRotacion,
    IconoPapelera,
    FormsModule,
    Paginacion,
    FocoModal,
    ImportarRotaciones,
  ],
  templateUrl: './listado-rotaciones.html',
  styleUrl: './listado-rotaciones.css',
})
export class ListadoRotaciones {
  private readonly api = inject(RotacionService);
  protected readonly consulta = new Consulta<Pagina<RotacionGestion>>((p) => this.api.gestionar(p));
  protected readonly catalogos = new Consulta<CatalogosRotacion>(() => this.api.catalogos());
  protected readonly filas = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly paginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly pagina = computed(() => this.consulta.datos()?.pagina ?? 1);
  protected readonly rotacionResidentes = signal<RotacionGestion | null>(null);
  protected readonly formulario = signal(false);
  protected readonly importando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly error = signal('');
  protected readonly pendiente = signal<{ fila: RotacionGestion; eliminar: boolean } | null>(null);
  protected edicion: RotacionGestion | null = null;
  protected termino = '';
  protected estado = '';
  protected servicio = '';
  protected asignaturaId = 0;
  protected inicio = '';
  protected fin = '';
  constructor() {
    this.cargar();
    this.catalogos.cargar();
  }
  protected cargar(pagina = 1, espera = 0) {
    const params: Parametros = { q: this.termino, pagina, tamano: 10 };
    if (this.estado) params['activa'] = this.estado;
    if (this.servicio) params['servicioId'] = this.servicio;
    this.consulta.cargar(params, espera);
  }
  protected async abrir(fila: RotacionGestion | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      this.edicion = fila ? await firstValueFrom(this.api.consultar(fila)) : null;
      this.asignaturaId = this.edicion?.asignaturaId ?? 0;
      this.inicio = this.edicion?.inicio ?? '';
      this.fin = this.edicion?.fin ?? '';
      this.formulario.set(true);
      this.catalogos.cargar();
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected async guardar() {
    if (this.guardando()) return;
    if (!this.asignaturaId || !this.inicio || !this.fin || this.fin < this.inicio) {
      this.error.set(
        'Completa la asignatura y las fechas; la fecha final no puede ser anterior a la inicial.',
      );
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    try {
      const datos = { asignaturaId: Number(this.asignaturaId), inicio: this.inicio, fin: this.fin };
      await firstValueFrom(
        this.edicion ? this.api.editar(this.edicion, datos) : this.api.crear(datos),
      );
      this.formulario.set(false);
      this.aviso.set(this.edicion ? 'Rotación actualizada.' : 'Rotación creada correctamente.');
      this.cargar();
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected async cambiarEstado(fila: RotacionGestion) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    this.aviso.set('');
    try {
      const actualizada = await firstValueFrom(this.api.estado(fila, !fila.activa));
      this.cargar(this.pagina());
      this.aviso.set(
        actualizada.activa ? 'Rotación activada.' : 'Rotación desactivada. Sus datos se conservan.',
      );
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected solicitar(fila: RotacionGestion, eliminar = true) {
    this.error.set('');
    this.pendiente.set({ fila, eliminar });
  }
  protected async confirmar() {
    const accion = this.pendiente();
    if (!accion || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      await firstValueFrom(this.api.eliminar(accion.fila));
      this.aviso.set('Rotación eliminada definitivamente.');
      this.pendiente.set(null);
      this.cargar(this.pagina());
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected cerrarImportacion() {
    this.importando.set(false);
    this.cargar();
  }
}
