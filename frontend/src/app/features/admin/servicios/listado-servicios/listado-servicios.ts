import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { Servicio, ServicioRequest } from '../../../../models/servicio.model';
import { mensajeError } from '../../../../services/api-error';
import { ServicioService } from '../../../../services/servicio.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FormularioServicio } from '../formulario-servicio/formulario-servicio';

@Component({
  selector: 'app-listado-servicios',
  imports: [Paginacion, FormularioServicio, IconoPapelera, FocoModal],
  templateUrl: './listado-servicios.html',
  styleUrl: './listado-servicios.css',
})
export class ListadoServicios {
  // Dependencias
  private readonly servicioService = inject(ServicioService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<Servicio>>((params) =>
    this.servicioService.listar(params),
  );
  protected readonly terminoBusqueda = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly indicePrimeraFila = computed(() => (this.paginaActual() - 1) * 10);
  protected readonly serviciosPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(() => this.consulta.cargando());
  protected readonly error = computed(() => this.consulta.error());
  protected readonly filtroEstado = signal('todos');
  protected readonly formularioAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly pendienteEliminar = signal<Servicio | null>(null);
  protected idEnEdicion: number | null = null;
  protected datosFormulario: ServicioRequest = {
    nombre: '',
    descripcion: '',
    cupoSimultaneo: null,
    cupoMaximoInstalado: null,
  };

  constructor() {
    this.cargar();
  }

  protected reintentar() {
    this.cargar();
  }

  protected cargar(espera = 0) {
    const params: Parametros = {
      q: this.terminoBusqueda(),
      pagina: this.paginaSolicitada(),
      tamano: 10,
    };
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

  protected async abrirFormulario(fila: Servicio | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      this.idEnEdicion = fila?.id ?? null;
      if (fila) {
        const detalle = await firstValueFrom(this.servicioService.consultar(fila.id));
        this.datosFormulario = {
          nombre: detalle.nombre,
          descripcion: detalle.descripcion ?? '',
          cupoSimultaneo: detalle.cupoSimultaneo,
          cupoMaximoInstalado: detalle.cupoMaximoInstalado,
        };
      } else
        this.datosFormulario = {
          nombre: '',
          descripcion: '',
          cupoSimultaneo: null,
          cupoMaximoInstalado: null,
        };
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
          ? this.servicioService.actualizar(this.idEnEdicion, this.datosFormulario)
          : this.servicioService.crear(this.datosFormulario),
      );
      this.formularioAbierto.set(false);
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
      await firstValueFrom(
        this.servicioService.cambiarEstado(fila.id, fila.activo ? 'Inactivo' : 'Activo'),
      );
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
      await firstValueFrom(this.servicioService.eliminarDefinitivamente(fila.id));
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
