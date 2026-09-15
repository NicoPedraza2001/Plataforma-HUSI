import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { UniversidadService } from '../../../../services/universidad.service';
import { UniversidadDetalle } from '../../../../models/universidad.model';
import { Consulta } from '../../../../shared/consulta/consulta';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { mensajeError } from '../../../../services/api-error';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';

@Component({
  selector: 'app-listado-universidades',
  imports: [IconoPapelera, FormsModule, RouterLink, Paginacion, FocoModal],
  templateUrl: './listado-universidades.html',
  styleUrl: './listado-universidades.css',
})
export class ListadoUniversidades {
  private readonly api = inject(UniversidadService);
  protected readonly consulta = new Consulta<Pagina<UniversidadDetalle>>((params) =>
    this.api.buscar(params),
  );
  protected readonly termino = signal('');
  protected readonly estado = signal('Todos');
  private readonly paginaSolicitada = signal(1);
  protected readonly pagina = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly filas = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly formulario = signal(false);
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
    const params: Parametros = { q: this.termino(), pagina: this.paginaSolicitada(), tamano: 10 };
    if (this.estado() !== 'Todos') params['estado'] = this.estado();
    this.consulta.cargar(params, espera);
  }
  protected buscar(valor: string) {
    this.termino.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }
  protected filtrarEstado(valor: string) {
    this.estado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }
  protected irAPagina(pagina: number) {
    if (this.consulta.cargando() || pagina < 1 || pagina > this.totalPaginas()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }
  protected abrir(universidad: UniversidadDetalle | null = null) {
    this.edicion = universidad;
    this.nombre = universidad?.nombre ?? '';
    this.pais = universidad?.pais ?? '';
    this.perfil = universidad?.perfil ?? '';
    this.errorFormulario.set('');
    this.aviso.set('');
    this.formulario.set(true);
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
        this.edicion ? this.api.actualizar(this.edicion.id, datos) : this.api.crear(datos),
      );
      this.aviso.set(
        this.edicion
          ? 'Universidad actualizada correctamente.'
          : 'Universidad creada correctamente.',
      );
      this.formulario.set(false);
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
        this.api.cambiarEstado(
          universidad.id,
          universidad.estado === 'Activo' ? 'Inactivo' : 'Activo',
        ),
      );
      this.consulta.datos.update((p) =>
        p
          ? { ...p, contenido: p.contenido.map((f) => (f.id === actualizada.id ? actualizada : f)) }
          : p,
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
      await firstValueFrom(this.api.eliminarDefinitivamente(universidad.id));
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
