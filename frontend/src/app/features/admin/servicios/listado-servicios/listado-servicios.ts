import { firstValueFrom } from 'rxjs';
import { ServicioRequest } from '../../../../models/servicio.model';
import { mensajeError } from '../../../../services/api-error';
import { FormularioServicio } from '../formulario-servicio/formulario-servicio';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { Component, computed, inject, signal } from '@angular/core';
import { ServicioService } from '../../../../services/servicio.service';
import { Servicio } from '../../../../models/servicio.model';
import { Consulta } from '../../../../shared/consulta/consulta';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
@Component({
  selector: 'app-listado-servicios',
  imports: [Paginacion, FormularioServicio, IconoPapelera, FocoModal],
  templateUrl: './listado-servicios.html',
  styleUrl: './listado-servicios.css',
})
export class ListadoServicios {
  private readonly api = inject(ServicioService);
  protected readonly consulta = new Consulta<Pagina<Servicio>>((params) => this.api.listar(params));
  protected readonly termino = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly desde = computed(() => (this.paginaActual() - 1) * 10);
  protected readonly serviciosPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(() => this.consulta.cargando());
  protected readonly error = computed(() => this.consulta.error());
  protected readonly estado = signal('todos');
  constructor() {
    this.cargar();
  }
  protected reintentar() {
    this.cargar();
  }
  protected cargar(espera = 0) {
    const params: Parametros = { q: this.termino(), pagina: this.paginaSolicitada(), tamano: 10 };
    if (this.estado() !== 'todos')
      params['estado'] = this.estado() === 'activos' ? 'Activo' : 'Inactivo';
    this.consulta.cargar(params, espera);
  }
  protected buscar(valor: string) {
    this.termino.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }
  protected filtrarPorEstado(valor: string) {
    this.estado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }
  protected irAPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas() || this.cargando()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }
  protected readonly formulario = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly pendienteEliminar = signal<Servicio | null>(null);
  protected edicionId: number | null = null;
  protected datos: ServicioRequest = {
    nombre: '',
    descripcion: '',
    cupoSimultaneo: null,
    cupoMaximoInstalado: null,
  };
  protected async abrir(fila: Servicio | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      this.edicionId = fila?.id ?? null;
      if (fila) {
        const detalle = await firstValueFrom(this.api.consultar(fila.id));
        this.datos = {
          nombre: detalle.nombre,
          descripcion: detalle.descripcion ?? '',
          cupoSimultaneo: detalle.cupoSimultaneo,
          cupoMaximoInstalado: detalle.cupoMaximoInstalado,
        };
      } else
        this.datos = {
          nombre: '',
          descripcion: '',
          cupoSimultaneo: null,
          cupoMaximoInstalado: null,
        };
      this.formulario.set(true);
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
        this.edicionId
          ? this.api.actualizar(this.edicionId, this.datos)
          : this.api.crear(this.datos),
      );
      this.formulario.set(false);
      this.aviso.set('Servicio guardado correctamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected async cambiarEstado(fila: Servicio) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      await firstValueFrom(this.api.cambiarEstado(fila.id, fila.activo ? 'Inactivo' : 'Activo'));
      this.aviso.set('Estado actualizado correctamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected solicitarEliminar(fila: Servicio) {
    this.errorFormulario.set('');
    this.pendienteEliminar.set(fila);
  }
  protected async confirmarEliminar() {
    const fila = this.pendienteEliminar();
    if (!fila || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(this.api.eliminarDefinitivamente(fila.id));
      this.pendienteEliminar.set(null);
      this.aviso.set('Servicio eliminado definitivamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
