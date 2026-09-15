import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Consulta, Pagina } from '../../../../services/consulta';
import { Component, computed, inject, signal } from '@angular/core';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, forkJoin, of } from 'rxjs';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UniversidadDetalle } from '../../../../services/universidades-api';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { ProgramaService as ProgramasApi } from '../../../../services/programa.service';
import { Programa } from '../../../../models/programa.model';
import { Universidad } from '../../../../models/universidad.model';
import { mensajeError } from '../../../../services/api-error';
@Component({
  selector: 'app-listado-programas',
  imports: [IconoPapelera, Paginacion, FormsModule, RouterLink, FocoModal],
  templateUrl: './listado-programas.html',
  styleUrl: './listado-programas.css',
})
export class ListadoProgramas {
  private readonly api = inject(ProgramasApi);
  private readonly route = inject(ActivatedRoute);
  protected readonly universidadRuta = signal<number | null>(null);
  protected readonly consulta = new Consulta<Pagina<Programa>>(params => this.universidadRuta() === null ? this.api.buscar(params) : this.api.listarUniversidad(this.universidadRuta()!, params));
  private readonly catalogo = new Consulta<{ universidades: Universidad[]; contexto: UniversidadDetalle | null }>(() => forkJoin({
    universidades: this.api.universidades(),
    contexto: this.universidadRuta() === null ? of(null) : this.api.consultarUniversidad(this.universidadRuta()!),
  }));
  protected readonly universidades = computed(() => this.catalogo.datos()?.universidades ?? []);
  protected readonly universidadContexto = computed(() => this.catalogo.datos()?.contexto ?? null);
  protected readonly contextoInactivo = computed(() => this.universidadContexto()?.estado === 'Inactivo');
  protected readonly termino = signal('');
  protected readonly paginaActual = signal(1);
  protected readonly cargando = computed(() => this.consulta.cargando() || this.catalogo.cargando());
  protected readonly guardando = signal(false);
  protected readonly error = computed(() => this.consulta.error() || this.catalogo.error());
  protected readonly aviso = signal('');
  protected readonly errorAccion = signal('');
  protected readonly formulario = signal(false);
  protected readonly pendienteEliminar = signal<Programa | null>(null);
  protected readonly errorFormulario = signal('');
  protected edicion: Programa | null = null;
  protected nombre = '';
  protected universidadId: number | null = null;
  protected readonly totalElementos = computed(() => this.consulta.datos()?.totalElementos ?? 0);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly paginaVisible = computed(() => this.consulta.datos()?.pagina ?? this.paginaActual());
  protected readonly desde = computed(() => (this.paginaVisible() - 1) * 10);
  protected readonly programasPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const id = params.get('universidadId');
      this.universidadRuta.set(id === null ? null : Number(id));
      this.termino.set(''); this.paginaActual.set(1);
      this.formulario.set(false); this.pendienteEliminar.set(null);
      this.cargar();
    });
  }
  protected cargar() {
    this.consulta.cargar({q: this.termino(), pagina: this.paginaActual(), tamano: 10});
    this.catalogo.cargar();
  }
  protected buscar(valor: string) {
    this.termino.set(valor); this.paginaActual.set(1);
    this.consulta.cargar({q: valor, pagina: 1, tamano: 10}, 300);
  }
  protected irAPagina(pagina: number) {
    if(pagina<1 || pagina>this.totalPaginas() || this.cargando()) return;
    this.paginaActual.set(pagina);
    this.consulta.cargar({q: this.termino(), pagina, tamano: 10});
  }
  protected abrir(programa: Programa | null = null) {
    this.edicion = programa;
    this.nombre = programa?.nombre ?? '';
    this.universidadId = programa?.universidadId ?? this.universidadRuta();
    this.errorFormulario.set('');
    this.formulario.set(true);
    this.aviso.set('');
  }
  protected async guardar() {
    if (this.guardando()) return;
    const nombre = this.nombre.trim();
    if (!nombre || nombre.length > 150 || !this.universidadId) {
      this.errorFormulario.set(
        'Escribe un nombre de hasta 150 caracteres y selecciona una universidad.',
      );
      return;
    }
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      const datos = { nombre, universidadId: this.universidadRuta() ?? this.universidadId };
      await firstValueFrom(
        this.edicion ? this.api.actualizar(this.edicion.id, datos) : this.api.crear(datos),
      );
      this.formulario.set(false);
      this.aviso.set(
        this.edicion ? 'Programa actualizado correctamente.' : 'Programa creado correctamente.',
      );
      await this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected async cambiarEstado(programa: Programa) {
    if(this.guardando()) return;
    this.guardando.set(true);this.errorAccion.set('');this.aviso.set('');
    try {
      const actualizado=await firstValueFrom(this.api.cambiarEstado(programa.id,programa.estado==='Activo'?'Inactivo':'Activo'));
      this.consulta.datos.update(p=>p?{...p,contenido:p.contenido.map(f=>f.id===actualizado.id?actualizado:f)}:p);
      this.aviso.set('Programa '+(actualizado.estado==='Activo'?'activado':'desactivado')+' correctamente.');
    } catch(e) {this.errorAccion.set(mensajeError(e));} finally {this.guardando.set(false);}
  }
  protected async confirmarEliminar() {
    const programa = this.pendienteEliminar();
    if (!programa || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(this.api.eliminarDefinitivamente(programa.id));
      this.pendienteEliminar.set(null);
      this.aviso.set(
        'Programa eliminado definitivamente de la base de datos.',
      );
      await this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected solicitarEliminar(programa: Programa) {
    this.errorFormulario.set('');
    this.pendienteEliminar.set(programa);
  }
}
