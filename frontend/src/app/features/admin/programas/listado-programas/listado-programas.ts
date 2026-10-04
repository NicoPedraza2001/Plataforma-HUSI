import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom, forkJoin, of } from 'rxjs';
import { Pagina } from '../../../../models/pagina.model';
import { Programa } from '../../../../models/programa.model';
import { Universidad, UniversidadDetalle } from '../../../../models/universidad.model';
import { mensajeError } from '../../../../services/api-error';
import { ProgramaService } from '../../../../services/programa.service';
import { UniversidadService } from '../../../../services/universidad.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FormularioPrograma } from '../formulario-programa/formulario-programa';

@Component({
  selector: 'app-listado-programas',
  imports: [FormularioPrograma, IconoPapelera, Paginacion, RouterLink, FocoModal],
  templateUrl: './listado-programas.html',
  styleUrl: './listado-programas.css',
})
export class ListadoProgramas {
  // Dependencias
  private readonly programaService = inject(ProgramaService);
  private readonly universidadService = inject(UniversidadService);
  private readonly route = inject(ActivatedRoute);

  // Estado de la pantalla y datos del formulario
  protected readonly universidadRuta = signal<number | null>(null);
  protected readonly consulta = new Consulta<Pagina<Programa>>((params) =>
    this.universidadRuta() === null
      ? this.programaService.buscar(params)
      : this.universidadService.programas(this.universidadRuta()!, params),
  );
  private readonly catalogo = new Consulta<{
    universidades: Universidad[];
    contexto: UniversidadDetalle | null;
  }>(() =>
    forkJoin({
      universidades: this.universidadService.listar(),
      contexto:
        this.universidadRuta() === null
          ? of(null)
          : this.universidadService.consultar(this.universidadRuta()!),
    }),
  );
  protected readonly universidades = computed(() => this.catalogo.datos()?.universidades ?? []);
  protected readonly universidadContexto = computed(() => this.catalogo.datos()?.contexto ?? null);
  protected readonly contextoInactivo = computed(
    () => this.universidadContexto()?.estado === 'Inactivo',
  );
  protected readonly terminoBusqueda = signal('');
  protected readonly paginaActual = signal(1);
  protected readonly cargando = computed(
    () => this.consulta.cargando() || this.catalogo.cargando(),
  );
  protected readonly guardando = signal(false);
  protected readonly error = computed(() => this.consulta.error() || this.catalogo.error());
  protected readonly aviso = signal('');
  protected readonly errorAccion = signal('');
  protected readonly formularioAbierto = signal(false);
  protected readonly pendienteEliminar = signal<Programa | null>(null);
  protected readonly errorFormulario = signal('');
  protected edicion: Programa | null = null;
  protected nombre = '';
  protected universidadId: number | null = null;
  protected readonly totalElementos = computed(() => this.consulta.datos()?.totalElementos ?? 0);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly paginaVisible = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaActual(),
  );
  protected readonly indicePrimeraFila = computed(() => (this.paginaVisible() - 1) * 10);
  protected readonly programasPagina = computed(() => this.consulta.datos()?.contenido ?? []);

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = params.get('universidadId');
      this.universidadRuta.set(id === null ? null : Number(id));
      this.terminoBusqueda.set('');
      this.paginaActual.set(1);
      this.formularioAbierto.set(false);
      this.pendienteEliminar.set(null);
      this.cargar();
    });
  }

  protected cargar() {
    this.consulta.cargar({ q: this.terminoBusqueda(), pagina: this.paginaActual(), tamano: 10 });
    this.catalogo.cargar();
  }

  protected buscar(valor: string) {
    this.terminoBusqueda.set(valor);
    this.paginaActual.set(1);
    this.consulta.cargar({ q: valor, pagina: 1, tamano: 10 }, 300);
  }

  protected irAPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas() || this.cargando()) return;
    this.paginaActual.set(pagina);
    this.consulta.cargar({ q: this.terminoBusqueda(), pagina, tamano: 10 });
  }

  protected abrirFormulario(programa: Programa | null = null) {
    this.edicion = programa;
    this.nombre = programa?.nombre ?? '';
    this.universidadId = programa?.universidadId ?? this.universidadRuta();
    this.errorFormulario.set('');
    this.formularioAbierto.set(true);
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
        this.edicion
          ? this.programaService.actualizar(this.edicion.id, datos)
          : this.programaService.crear(datos),
      );
      this.formularioAbierto.set(false);
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
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorAccion.set('');
    this.aviso.set('');
    try {
      const actualizado = await firstValueFrom(
        this.programaService.cambiarEstado(
          programa.id,
          programa.estado === 'Activo' ? 'Inactivo' : 'Activo',
        ),
      );
      this.consulta.datos.update((pagina) =>
        pagina
          ? {
              ...pagina,
              contenido: pagina.contenido.map((registro) =>
                registro.id === actualizado.id ? actualizado : registro,
              ),
            }
          : pagina,
      );
      this.aviso.set(
        'Programa ' +
          (actualizado.estado === 'Activo' ? 'activado' : 'desactivado') +
          ' correctamente.',
      );
    } catch (e) {
      this.errorAccion.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async confirmarEliminar() {
    const programa = this.pendienteEliminar();
    if (!programa || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(this.programaService.eliminarDefinitivamente(programa.id));
      this.pendienteEliminar.set(null);
      this.aviso.set('Programa eliminado definitivamente de la base de datos.');
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
