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
  private readonly residenteService = inject(ResidenteService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly programaService = inject(ProgramaService);
  private readonly tipoDocumentoService = inject(TipoDocumentoService);
  private readonly perfilAcademicoService = inject(PerfilAcademicoService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<Residente>>((params) =>
    this.residenteService.listar(params),
  );
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogoService.listar());
  protected readonly terminoBusqueda = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly indicePrimeraFila = computed(() => (this.paginaActual() - 1) * 8);
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
  protected readonly formularioAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly opciones = signal<OpcionCatalogo[]>([]);
  protected readonly tiposDocumento = signal<TipoDocumento[]>([]);
  protected readonly perfiles = signal<OpcionCatalogo[]>([]);
  protected readonly pendienteEliminar = signal<Residente | null>(null);
  protected idEnEdicion: number | null = null;
  protected datosFormulario: ResidenteRequest = {
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
    const params: Parametros = {
      q: this.terminoBusqueda(),
      pagina: this.paginaSolicitada(),
      tamano: 8,
    };
    if (this.universidadSeleccionada() !== 'Todas')
      params['universidad'] = this.universidadSeleccionada();
    if (this.programaSeleccionado() !== 'Todos') params['programa'] = this.programaSeleccionado();
    if (this.estadoSeleccionado() !== 'Todos') params['estado'] = this.estadoSeleccionado();
    if (this.periodoSeleccionado() !== 'Todos') params['periodo'] = this.periodoSeleccionado();
    this.consulta.cargar(params, espera);
  }

  protected buscar(valor: string) {
    this.terminoBusqueda.set(valor);
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

  protected async abrirFormulario(fila: Residente | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      const [programas, tipos, perfiles] = await Promise.all([
        firstValueFrom(this.programaService.listar()),
        firstValueFrom(this.tipoDocumentoService.listar()),
        firstValueFrom(this.perfilAcademicoService.listar()),
      ]);
      const opciones = programas.map((programa) => ({
        id: programa.id,
        nombre: programa.nombre + ' — ' + programa.universidad,
      }));
      this.idEnEdicion = fila?.id ?? null;
      if (fila) {
        const detalle = await firstValueFrom(this.residenteService.consultar(fila.id));
        if (!opciones.some((opcion) => opcion.id === detalle.programaId)) {
          const programaActual = await firstValueFrom(
            this.programaService.consultar(detalle.programaId!),
          );
          opciones.push({
            id: programaActual.id,
            nombre: programaActual.nombre + ' — ' + programaActual.universidad + ' (actual)',
          });
        }
        // El backend omite los campos nulos: sin perfil llega undefined.
        this.datosFormulario = { ...detalle, perfilAcademico: detalle.perfilAcademico ?? '' };
      } else
        this.datosFormulario = {
          nombre: '',
          tipoDocumento: 'CC',
          numeroDocumento: '',
          perfilAcademico: '',
          programaId: null,
        };
      this.opciones.set(opciones);
      this.tiposDocumento.set(tipos);
      this.perfiles.set(perfiles);
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
          ? this.residenteService.actualizar(this.idEnEdicion, this.datosFormulario)
          : this.residenteService.crear(this.datosFormulario),
      );
      this.formularioAbierto.set(false);
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
        this.residenteService.cambiarEstado(
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
      await firstValueFrom(this.residenteService.eliminarDefinitivamente(fila.id));
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
