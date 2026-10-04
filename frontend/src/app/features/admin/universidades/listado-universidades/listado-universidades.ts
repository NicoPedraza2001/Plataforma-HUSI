import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { UniversidadDetalle } from '../../../../models/universidad.model';
import { mensajeError } from '../../../../services/api-error';
import { UniversidadService } from '../../../../services/universidad.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FormularioUniversidad } from '../formulario-universidad/formulario-universidad';

@Component({
  selector: 'app-listado-universidades',
  imports: [FormularioUniversidad, IconoPapelera, RouterLink, Paginacion, FocoModal],
  templateUrl: './listado-universidades.html',
  styleUrl: './listado-universidades.css',
})
export class ListadoUniversidades {
  // Dependencias
  private readonly universidadService = inject(UniversidadService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<UniversidadDetalle>>((params) =>
    this.universidadService.buscar(params),
  );
  protected readonly terminoBusqueda = signal('');
  protected readonly filtroEstado = signal('Todos');
  private readonly paginaSolicitada = signal(1);
  protected readonly pagina = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly filas = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly formularioAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly pendienteEliminar = signal<UniversidadDetalle | null>(null);
  protected edicion: UniversidadDetalle | null = null;
  protected nombre = '';
  protected pais = '';
  protected perfil = '';

  constructor() {
    this.cargar();
  }

  protected cargar(espera = 0) {
    const params: Parametros = {
      q: this.terminoBusqueda(),
      pagina: this.paginaSolicitada(),
      tamano: 10,
    };
    if (this.filtroEstado() !== 'Todos') params['estado'] = this.filtroEstado();
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
    if (this.consulta.cargando() || pagina < 1 || pagina > this.totalPaginas()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }

  protected abrirFormulario(universidad: UniversidadDetalle | null = null) {
    this.edicion = universidad;
    this.nombre = universidad?.nombre ?? '';
    this.pais = universidad?.pais ?? '';
    this.perfil = universidad?.perfil ?? '';
    this.errorFormulario.set('');
    this.aviso.set('');
    this.formularioAbierto.set(true);
  }

  protected async guardar() {
    if (this.guardando()) return;
    const datos = {
      nombre: this.nombre.trim(),
      pais: this.pais.trim(),
      perfil: this.perfil.trim() || null,
    };
    if (
      !datos.nombre ||
      !datos.pais ||
      datos.nombre.length > 150 ||
      datos.pais.length > 100 ||
      (datos.perfil?.length ?? 0) > 50
    ) {
      this.errorFormulario.set(
        'Completa nombre y país. Máximos: nombre 150, país 100 y perfil 50 caracteres.',
      );
      return;
    }
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(
        this.edicion
          ? this.universidadService.actualizar(this.edicion.id, datos)
          : this.universidadService.crear(datos),
      );
      this.aviso.set(
        this.edicion
          ? 'Universidad actualizada correctamente.'
          : 'Universidad creada correctamente.',
      );
      this.formularioAbierto.set(false);
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async cambiarEstado(universidad: UniversidadDetalle) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      const actualizada = await firstValueFrom(
        this.universidadService.cambiarEstado(
          universidad.id,
          universidad.estado === 'Activo' ? 'Inactivo' : 'Activo',
        ),
      );
      this.consulta.datos.update((pagina) =>
        pagina
          ? {
              ...pagina,
              contenido: pagina.contenido.map((registro) =>
                registro.id === actualizada.id ? actualizada : registro,
              ),
            }
          : pagina,
      );
      this.aviso.set(
        'Universidad ' +
          (actualizada.estado === 'Activo' ? 'activada' : 'desactivada') +
          ' correctamente.',
      );
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected solicitarEliminar(universidad: UniversidadDetalle) {
    this.errorFormulario.set('');
    this.pendienteEliminar.set(universidad);
  }

  protected async confirmarEliminar() {
    const universidad = this.pendienteEliminar();
    if (!universidad || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(this.universidadService.eliminarDefinitivamente(universidad.id));
      this.pendienteEliminar.set(null);
      this.aviso.set('Universidad eliminada definitivamente de la base de datos.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
