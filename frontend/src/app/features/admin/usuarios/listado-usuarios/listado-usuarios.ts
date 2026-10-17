import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Catalogos } from '../../../../models/catalogos.model';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { Usuario, UsuarioRequest } from '../../../../models/usuario.model';
import { mensajeError } from '../../../../services/api-error';
import { CatalogoService } from '../../../../services/catalogo.service';
import { RolService } from '../../../../services/rol.service';
import { ServicioService } from '../../../../services/servicio.service';
import { UsuarioService } from '../../../../services/usuario.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { FormularioUsuario } from '../formulario-usuario/formulario-usuario';
import { generarContrasena } from '../formulario-usuario/generar-contrasena';

@Component({
  selector: 'app-listado-usuarios',
  imports: [Paginacion, FormularioUsuario, IconoPapelera, FocoModal],
  templateUrl: './listado-usuarios.html',
  styleUrl: './listado-usuarios.css',
})
export class ListadoUsuarios {
  // Dependencias
  private readonly usuarioService = inject(UsuarioService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly rolService = inject(RolService);
  private readonly servicioService = inject(ServicioService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<Usuario>>((params) =>
    this.usuarioService.listar(params),
  );
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogoService.listar());
  protected readonly terminoBusqueda = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly indicePrimeraFila = computed(() => (this.paginaActual() - 1) * 8);
  protected readonly usuariosPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(
    () => this.consulta.cargando() || this.catalogos.cargando(),
  );
  protected readonly error = computed(() => this.consulta.error() || this.catalogos.error());
  protected readonly rol = signal('todos');
  protected readonly filtroEstado = signal('todos');
  protected readonly roles = computed(() => this.catalogos.datos()?.roles ?? []);
  protected readonly formularioAbierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly opciones = signal<OpcionCatalogo[]>([]);
  protected readonly servicios = signal<OpcionCatalogo[]>([]);
  protected readonly pendienteEliminar = signal<Usuario | null>(null);
  protected idEnEdicion: number | null = null;
  protected datosFormulario: UsuarioRequest = {
    nombre: '',
    apellido: '',
    correo: '',
    contrasena: '',
    rolId: null,
    servicioId: null,
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
    if (this.rol() !== 'todos') params['rol'] = this.rol();
    if (this.filtroEstado() !== 'todos')
      params['estado'] = this.filtroEstado() === 'activos' ? 'Activo' : 'Inactivo';
    this.consulta.cargar(params, espera);
  }

  protected buscar(valor: string) {
    this.terminoBusqueda.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }

  protected filtrarPorRol(valor: string) {
    this.rol.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
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

  protected async abrirFormulario(fila: Usuario | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      const [opciones, serviciosActivos] = await Promise.all([
        firstValueFrom(this.rolService.listar()),
        firstValueFrom(this.servicioService.catalogo()),
      ]);
      this.idEnEdicion = fila?.id ?? null;
      let servicios = serviciosActivos;
      if (fila) {
        const detalle = await firstValueFrom(this.usuarioService.consultar(fila.id));
        this.datosFormulario = {
          nombre: detalle.nombre,
          apellido: detalle.apellido,
          correo: detalle.correo,
          contrasena: '',
          rolId: detalle.rolId,
          servicioId: detalle.servicioId,
        };
        servicios = this.conServicioActual(serviciosActivos, detalle.servicioId, detalle.servicio);
      } else
        this.datosFormulario = {
          nombre: '',
          apellido: '',
          correo: '',
          contrasena: generarContrasena(),
          rolId: null,
          servicioId: null,
        };
      this.opciones.set(opciones);
      this.servicios.set(servicios);
      this.formularioAbierto.set(true);
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  /**
   * El catálogo solo trae servicios activos. Si el coordinador ya tenía uno que se desactivó, se
   * agrega para que el formulario lo muestre y se pueda conservar (el backend lo permite).
   */
  private conServicioActual(
    activos: OpcionCatalogo[],
    servicioId: number | null,
    servicio: string | null,
  ): OpcionCatalogo[] {
    if (servicioId === null || activos.some((opcion) => opcion.id === servicioId)) return activos;
    return [...activos, { id: servicioId, nombre: `${servicio} (inactivo)` }];
  }

  protected async guardar() {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(
        this.idEnEdicion
          ? this.usuarioService.actualizar(this.idEnEdicion, this.datosFormulario)
          : this.usuarioService.crear(this.datosFormulario),
      );
      this.formularioAbierto.set(false);
      this.aviso.set('Usuario guardado correctamente.');
      this.cargar();
      this.catalogos.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async cambiarEstado(fila: Usuario) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      await firstValueFrom(
        this.usuarioService.cambiarEstado(fila.id, fila.activo ? 'Inactivo' : 'Activo'),
      );
      this.aviso.set('Estado actualizado correctamente.');
      this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected solicitarEliminar(fila: Usuario) {
    this.errorFormulario.set('');
    this.pendienteEliminar.set(fila);
  }

  protected async confirmarEliminar() {
    const fila = this.pendienteEliminar();
    if (!fila || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(this.usuarioService.eliminarDefinitivamente(fila.id));
      this.pendienteEliminar.set(null);
      this.aviso.set('Usuario eliminado definitivamente.');
      this.cargar();
      this.catalogos.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
