import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Asignatura, AsignaturaRequest } from '../../../../models/asignatura.model';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { mensajeError } from '../../../../services/api-error';
import { AsignaturaService } from '../../../../services/asignatura.service';
import { ServicioService } from '../../../../services/servicio.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FormularioAsignatura } from '../formulario-asignatura/formulario-asignatura';

@Component({
  selector: 'app-listado-asignaturas',
  imports: [Paginacion, FormularioAsignatura, IconoPapelera, FocoModal],
  templateUrl: './listado-asignaturas.html',
  styleUrl: './listado-asignaturas.css',
})
export class ListadoAsignaturas {
  // Dependencias
  private readonly asignaturaService = inject(AsignaturaService);
  private readonly servicioService = inject(ServicioService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<Asignatura>>((params) =>
    this.asignaturaService.listar(params),
  );
  protected readonly terminoBusqueda = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly asignaturasPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(() => this.consulta.cargando());
  protected readonly error = computed(() => this.consulta.error());
  protected readonly filtroEstado = signal('todos');
  protected readonly opcionesServicio = signal<OpcionCatalogo[]>([]);
  protected readonly serviciosFiltro = signal<OpcionCatalogo[]>([]);
  protected readonly servicioFiltro = signal('');
  protected readonly errorCatalogo = signal('');
  protected readonly formularioAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly pendienteEliminar = signal<Asignatura | null>(null);
  protected idEnEdicion: number | null = null;
  protected datosFormulario: AsignaturaRequest = { nombre: '', servicioId: null };

  constructor() {
    this.cargar();
    void this.cargarCatalogoFiltro();
  }

  protected filtrarServicio(valor: string) {
    this.servicioFiltro.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }

  protected reintentar() {
    void this.cargarCatalogoFiltro();
    this.cargar();
  }

  protected cargar(espera = 0) {
    const params: Parametros = {
      q: this.terminoBusqueda(),
      pagina: this.paginaSolicitada(),
      tamano: 10,
    };
    if (this.servicioFiltro()) params['servicioId'] = this.servicioFiltro();
    if (this.filtroEstado() !== 'todos')
      params['estado'] = this.filtroEstado() === 'activos' ? 'Activo' : 'Inactivo';
    this.consulta.cargar(params, espera);
  }

  protected buscar(valor: string) {
    this.terminoBusqueda.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }

  protected filtrarPorEstado(valor: string) {
    this.filtroEstado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }

  protected irAPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas() || this.cargando()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }

  protected async abrirFormulario(fila: Asignatura | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      this.idEnEdicion = fila?.id ?? null;
      this.opcionesServicio.set(await firstValueFrom(this.servicioService.catalogo()));

      if (fila) {
        const detalle = await firstValueFrom(this.asignaturaService.consultar(fila.id));
        this.datosFormulario = { nombre: detalle.nombre, servicioId: detalle.servicioId };
        if (!this.opcionesServicio().some((opcion) => opcion.id === detalle.servicioId))
          this.opcionesServicio.update((opcionesActuales) => [
            ...opcionesActuales,
            { id: detalle.servicioId, nombre: detalle.servicio + ' (actual)' },
          ]);
      } else this.datosFormulario = { nombre: '', servicioId: null };
      this.formularioAbierto.set(true);
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async guardar() {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(
        this.idEnEdicion
          ? this.asignaturaService.actualizar(this.idEnEdicion, this.datosFormulario)
          : this.asignaturaService.crear(this.datosFormulario),
      );
      this.formularioAbierto.set(false);
      this.aviso.set('Asignatura guardada correctamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async cambiarEstado(fila: Asignatura) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      await firstValueFrom(
        this.asignaturaService.cambiarEstado(fila.id, fila.activo ? 'Inactivo' : 'Activo'),
      );
      this.aviso.set('Estado actualizado correctamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected solicitarEliminar(fila: Asignatura) {
    this.errorFormulario.set('');
    this.pendienteEliminar.set(fila);
  }

  protected async confirmarEliminar() {
    const fila = this.pendienteEliminar();
    if (!fila || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(this.asignaturaService.eliminarDefinitivamente(fila.id));
      this.pendienteEliminar.set(null);
      this.aviso.set('Asignatura eliminada definitivamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  private async cargarCatalogoFiltro() {
    this.errorCatalogo.set('');
    try {
      this.serviciosFiltro.set(await firstValueFrom(this.servicioService.catalogo(false)));
    } catch (e) {
      this.errorCatalogo.set(mensajeError(e));
    }
  }
}
