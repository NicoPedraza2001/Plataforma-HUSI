import { firstValueFrom } from 'rxjs';
import { UsuarioRequest } from '../../../../models/usuario.model';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { mensajeError } from '../../../../services/api-error';
import { FormularioUsuario } from '../formulario-usuario/formulario-usuario';
import { IconoPapelera } from '../../../../shared/icono-papelera/icono-papelera';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { RolService } from '../../../../services/rol.service';
import { Component, computed, inject, signal } from '@angular/core';
import { UsuarioService } from '../../../../services/usuario.service';
import { Usuario } from '../../../../models/usuario.model';
import { Consulta } from '../../../../shared/consulta/consulta';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { Catalogos } from '../../../../models/catalogos.model';
import { CatalogoService } from '../../../../services/catalogo.service';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
@Component({
  selector: 'app-listado-usuarios',
  imports: [Paginacion, FormularioUsuario, IconoPapelera, FocoModal],
  templateUrl: './listado-usuarios.html',
  styleUrl: './listado-usuarios.css',
})
export class ListadoUsuarios {
  private readonly api = inject(UsuarioService);
  private readonly catalogosApi = inject(CatalogoService);
  protected readonly consulta = new Consulta<Pagina<Usuario>>((params) => this.api.listar(params));
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogosApi.listar());
  protected readonly termino = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly desde = computed(() => (this.paginaActual() - 1) * 8);
  protected readonly usuariosPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(
    () => this.consulta.cargando() || this.catalogos.cargando(),
  );
  protected readonly error = computed(() => this.consulta.error() || this.catalogos.error());
  protected readonly rol = signal('todos');
  protected readonly estado = signal('todos');
  protected readonly roles = computed(() => this.catalogos.datos()?.roles ?? []);
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
    if (this.rol() !== 'todos') params['rol'] = this.rol();
    if (this.estado() !== 'todos')
      params['estado'] = this.estado() === 'activos' ? 'Activo' : 'Inactivo';
    this.consulta.cargar(params, espera);
  }
  protected buscar(valor: string) {
    this.termino.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }
  protected filtrarPorRol(valor: string) {
    this.rol.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
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

  private readonly opcionesApi = inject(RolService);
  protected readonly formulario = signal(false);
  protected readonly guardando = signal(false);
  protected readonly aviso = signal('');
  protected readonly errorFormulario = signal('');
  protected readonly opciones = signal<OpcionCatalogo[]>([]);
  protected readonly pendienteEliminar = signal<Usuario | null>(null);
  protected edicionId: number | null = null;
  protected datos: UsuarioRequest = {
    nombre: '',
    apellido: '',
    correo: '',
    contrasena: '',
    rolId: null,
  };
  protected async abrir(fila: Usuario | null = null) {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    this.aviso.set('');
    try {
      const opciones = await firstValueFrom(this.opcionesApi.listar());
      this.edicionId = fila?.id ?? null;
      if (fila) {
        const detalle = await firstValueFrom(this.api.consultar(fila.id));
        this.datos = { ...detalle, contrasena: '' };
      } else this.datos = { nombre: '', apellido: '', correo: '', contrasena: '', rolId: null };
      this.opciones.set(opciones);
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
      await firstValueFrom(this.api.cambiarEstado(fila.id, fila.activo ? 'Inactivo' : 'Activo'));
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
      await firstValueFrom(this.api.eliminarDefinitivamente(fila.id));
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
