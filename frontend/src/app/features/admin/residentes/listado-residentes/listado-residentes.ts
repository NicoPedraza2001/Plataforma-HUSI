import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Catalogos } from '../../../../models/catalogos.model';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { Residente, ResidenteRequest } from '../../../../models/residente.model';
import { TipoDocumento } from '../../../../models/tipo-documento.model';
import { mensajeError } from '../../../../services/api-error';
import { CatalogoService } from '../../../../services/catalogo.service';
import { PerfilAcademicoService } from '../../../../services/perfil-academico.service';
import { ProgramaService } from '../../../../services/programa.service';
import { ResidenteService } from '../../../../services/residente.service';
import { TipoDocumentoService } from '../../../../services/tipo-documento.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FormularioResidente } from '../formulario-residente/formulario-residente';

@Component({
  selector: 'app-listado-residentes',
  imports: [Paginacion, FormularioResidente, IconoPapelera, FocoModal],
  templateUrl: './listado-residentes.html',
  styleUrl: './listado-residentes.css',
})
export class ListadoResidentes {
  // Dependencias
  private readonly api = inject(ResidenteService);
  private readonly catalogosApi = inject(CatalogoService);
  private readonly opcionesApi = inject(ProgramaService);
  private readonly tiposDocumentoApi = inject(TipoDocumentoService);
  private readonly perfilesApi = inject(PerfilAcademicoService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<Residente>>((params) =>
    this.api.listar(params),
  );
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogosApi.listar());
  protected readonly termino = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly desde = computed(() => (this.paginaActual() - 1) * 8);
  protected readonly residentesPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(
    () => this.consulta.cargando() || this.catalogos.cargando(),
  );
  protected readonly error = computed(() => this.consulta.error() || this.catalogos.error());
  protected readonly universidadSeleccionada = signal('Todas');
  protected readonly programaSeleccionado = signal('Todos');
  protected readonly estadoSeleccionado = signal('Todos');
  protected readonly periodoSeleccionado = signal('Todos');
  protected readonly universidades = computed(() => [
    'Todas',
    ...(this.catalogos.datos()?.universidades ?? []),
  ]);
  protected readonly programas = computed(() => [
    'Todos',
    ...(this.catalogos.datos()?.programas ?? []),
  ]);
  protected readonly periodos = computed(() => [
    'Todos',
    ...(this.catalogos.datos()?.periodos ?? []),
  ]);
  protected readonly estados = ['Todos', 'ACTIVO', 'INACTIVO'];
  protected readonly formulario = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly opciones = signal<OpcionCatalogo[]>([]);
  protected readonly tiposDocumento = signal<TipoDocumento[]>([]);
  protected readonly perfiles = signal<OpcionCatalogo[]>([]);
  protected readonly pendienteEliminar = signal<Residente | null>(null);
  protected edicionId: number | null = null;
  protected datos: ResidenteRequest = {
    nombre: '',
    tipoDocumento: 'CC',
    numeroDocumento: '',
    perfilAcademico: '',
    programaId: null,
  };

  constructor() {
    this.cargar();
    this.catalogos.cargar();
  }

  protected reintentar() {
    this.cargar();
    this.catalogos.cargar();
  }

  protected cargar(espera = 0) {
    const params: Parametros = { q: this.termino(), pagina: this.paginaSolicitada(), tamano: 8 };
    if (this.universidadSeleccionada() !== 'Todas')
      params['universidad'] = this.universidadSeleccionada();
    if (this.programaSeleccionado() !== 'Todos') params['programa'] = this.programaSeleccionado();
    if (this.estadoSeleccionado() !== 'Todos') params['estado'] = this.estadoSeleccionado();
    if (this.periodoSeleccionado() !== 'Todos') params['periodo'] = this.periodoSeleccionado();
    this.consulta.cargar(params, espera);
  }

  protected buscar(valor: string) {
    this.termino.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }

  protected onUniversidadChange(valor: string) {
    this.universidadSeleccionada.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }

  protected onProgramaChange(valor: string) {
    this.programaSeleccionado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }

  protected onEstadoChange(valor: string) {
    this.estadoSeleccionado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }

  protected onPeriodoChange(valor: string) {
    this.periodoSeleccionado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }

  protected irAPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas() || this.cargando()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }

  protected async abrir(fila: Residente | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      const [programas, tipos, perfiles] = await Promise.all([
        firstValueFrom(this.opcionesApi.listar()),
        firstValueFrom(this.tiposDocumentoApi.listar()),
        firstValueFrom(this.perfilesApi.listar()),
      ]);
      const opciones = programas.map((p) => ({
        id: p.id,
        nombre: p.nombre + ' — ' + p.universidad,
      }));
      this.edicionId = fila?.id ?? null;
      if (fila) {
        const detalle = await firstValueFrom(this.api.consultar(fila.id));
        if (!opciones.some((o) => o.id === detalle.programaId)) {
          const p = await firstValueFrom(this.opcionesApi.consultar(detalle.programaId!));
          opciones.push({ id: p.id, nombre: p.nombre + ' — ' + p.universidad + ' (actual)' });
        }
        // El backend omite los campos nulos: sin perfil llega undefined.
        this.datos = { ...detalle, perfilAcademico: detalle.perfilAcademico ?? '' };
      } else
        this.datos = {
          nombre: '',
          tipoDocumento: 'CC',
          numeroDocumento: '',
          perfilAcademico: '',
          programaId: null,
        };
      this.opciones.set(opciones);
      this.tiposDocumento.set(tipos);
      this.perfiles.set(perfiles);
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
      this.aviso.set('Residente guardado correctamente.');
      this.cargar();
      this.catalogos.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async cambiarEstado(fila: Residente) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      await firstValueFrom(
        this.api.cambiarEstado(
          fila.id,
          fila.estado.toLowerCase() === 'activo' ? 'Inactivo' : 'Activo',
        ),
      );
      this.aviso.set('Estado actualizado correctamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected solicitarEliminar(fila: Residente) {
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
      this.aviso.set('Residente eliminado definitivamente.');
      this.cargar();
      this.catalogos.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
