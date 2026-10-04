import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { CatalogosRotacion, RotacionGestion } from '../../../../models/rotacion.model';
import { mensajeError } from '../../../../services/api-error';
import { RotacionService } from '../../../../services/rotacion.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FormularioRotacion } from '../formulario-rotacion/formulario-rotacion';
import { hoyBogota } from '../hoy';
import { ResidentesRotacion } from '../residentes-rotacion/residentes-rotacion';

@Component({
  selector: 'app-listado-rotaciones',
  imports: [
    FormularioRotacion,
    ResidentesRotacion,
    IconoPapelera,
    FormsModule,
    Paginacion,
    FocoModal,
    DatePipe,
  ],
  templateUrl: './listado-rotaciones.html',
  styleUrl: './listado-rotaciones.css',
})
export class ListadoRotaciones {
  // Dependencias
  private readonly rotacionService = inject(RotacionService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<RotacionGestion>>((parametros) =>
    this.rotacionService.listarGestion(parametros),
  );
  protected readonly catalogos = new Consulta<CatalogosRotacion>(() =>
    this.rotacionService.catalogos(),
  );
  protected readonly filas = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly paginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly pagina = computed(() => this.consulta.datos()?.pagina ?? 1);
  protected readonly rotacionVerResidentes = signal<RotacionGestion | null>(null);
  protected readonly formularioAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly error = signal('');
  protected readonly accionPendiente = signal<{ fila: RotacionGestion; eliminar: boolean } | null>(
    null,
  );
  protected rotacionEnEdicion: RotacionGestion | null = null;
  protected terminoBusqueda = '';
  protected filtroEstado = '';
  protected filtroServicioId = '';
  protected asignaturaId = 0;
  protected inicio = '';
  protected fin = '';

  constructor() {
    this.cargar();
    this.catalogos.cargar();
  }

  protected cargar(pagina = 1, espera = 0) {
    const params: Parametros = { q: this.terminoBusqueda, pagina, tamano: 10 };
    if (this.filtroEstado) params['activa'] = this.filtroEstado;
    if (this.filtroServicioId) params['servicioId'] = this.filtroServicioId;
    this.consulta.cargar(params, espera);
  }

  protected async abrirFormulario(fila: RotacionGestion | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      this.rotacionEnEdicion = fila
        ? await firstValueFrom(this.rotacionService.consultar(fila))
        : null;
      this.asignaturaId = this.rotacionEnEdicion?.asignaturaId ?? 0;
      this.inicio = this.rotacionEnEdicion?.inicio ?? '';
      this.fin = this.rotacionEnEdicion?.fin ?? '';
      this.formularioAbierto.set(true);
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
    const fechasCambian =
      this.inicio !== this.rotacionEnEdicion?.inicio || this.fin !== this.rotacionEnEdicion?.fin;
    if (fechasCambian && this.inicio < hoyBogota()) {
      this.error.set('La fecha de inicio no puede ser anterior a hoy.');
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    try {
      const datos = { asignaturaId: Number(this.asignaturaId), inicio: this.inicio, fin: this.fin };
      const guardada = await firstValueFrom(
        this.rotacionEnEdicion
          ? this.rotacionService.editar(this.rotacionEnEdicion, datos)
          : this.rotacionService.crear(datos),
      );
      this.formularioAbierto.set(false);
      this.aviso.set(
        this.rotacionEnEdicion ? 'Rotación actualizada.' : 'Rotación creada correctamente.',
      );
      this.cargar();
      if (!this.rotacionEnEdicion) await this.ofrecerTrasladoDeEspera(guardada);
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  /** Si hay residentes esperando cupo en el servicio, abre la rotación nueva para trasladarlos. */
  private async ofrecerTrasladoDeEspera(rotacion: RotacionGestion) {
    try {
      const espera = await firstValueFrom(this.rotacionService.listarEsperaDelServicio(rotacion));
      if (!espera.length) return;
      this.aviso.set(
        'Rotación creada. Hay residentes esperando cupo en este servicio: elige cuáles trasladar.',
      );
      this.rotacionVerResidentes.set(rotacion);
    } catch {
      // La rotación ya se creó; la espera puede revisarse desde "Ver residentes".
    }
  }

  protected async cambiarEstado(fila: RotacionGestion) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    this.aviso.set('');
    try {
      const actualizada = await firstValueFrom(
        this.rotacionService.cambiarEstado(fila, !fila.activa),
      );
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

  protected solicitarEliminar(fila: RotacionGestion, eliminar = true) {
    this.error.set('');
    this.accionPendiente.set({ fila, eliminar });
  }

  protected async confirmar() {
    const accion = this.accionPendiente();
    if (!accion || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      await firstValueFrom(this.rotacionService.eliminar(accion.fila));
      this.aviso.set('Rotación eliminada definitivamente.');
      this.accionPendiente.set(null);
      this.cargar(this.pagina());
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
