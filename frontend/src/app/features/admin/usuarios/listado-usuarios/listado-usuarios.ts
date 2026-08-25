import { Component, computed, signal } from '@angular/core';
import { Paginacion } from '../../../../shared/paginacion/paginacion';

/** Fila del listado de usuarios de la plataforma. */
export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  activo: boolean;
}

/** Valor del filtro de estado; 'todos' desactiva el filtro. */
type FiltroEstado = 'todos' | 'activos' | 'inactivos';

/** Filas por página del listado. */
const TAMANO_PAGINA = 8;

// TODO: reemplazar por los datos que entregue el backend.
const USUARIOS_DEMO: readonly Usuario[] = [
  { id: 1, nombre: 'Laura Martínez', correo: 'laura.martinez@husi.org.co', rol: 'Administrador', activo: true },
  { id: 2, nombre: 'Gabriela Pérez', correo: 'gabriela.perez@husi.org.co', rol: 'Doctor', activo: true },
  { id: 3, nombre: 'Juan Carvajal', correo: 'juan.carvajal@javeriana.edu.co', rol: 'Residente', activo: true },
  { id: 4, nombre: 'Camila Torres', correo: 'camila.torres@husi.org.co', rol: 'Oficina de Educación', activo: true },
  { id: 5, nombre: 'Andrés Gómez', correo: 'andres.gomez@andes.edu.co', rol: 'Observer', activo: false },
  { id: 6, nombre: 'Daniela Lizarazo', correo: 'daniela.lizarazo@urosario.edu.co', rol: 'Residente', activo: true },
  { id: 7, nombre: 'Miguel Herrera', correo: 'miguel.herrera@husi.org.co', rol: 'Doctor', activo: true },
  { id: 8, nombre: 'Paula Rincón', correo: 'paula.rincon@husi.org.co', rol: 'Soporte técnico', activo: true },
  { id: 9, nombre: 'Santiago Ruiz', correo: 'santiago.ruiz@husi.org.co', rol: 'Doctor', activo: true },
  { id: 10, nombre: 'Valentina Ospina', correo: 'valentina.ospina@javeriana.edu.co', rol: 'Residente', activo: false },
  { id: 11, nombre: 'Carlos Beltrán', correo: 'carlos.beltran@husi.org.co', rol: 'Administrador', activo: true },
  { id: 12, nombre: 'Mariana Suárez', correo: 'mariana.suarez@husi.org.co', rol: 'Oficina de Educación', activo: true },
  { id: 13, nombre: 'Felipe Navarro', correo: 'felipe.navarro@unal.edu.co', rol: 'Observer', activo: true },
  { id: 14, nombre: 'Lucía Rodríguez', correo: 'lucia.rodriguez@husi.org.co', rol: 'Soporte técnico', activo: false },
];

@Component({
  selector: 'app-listado-usuarios',
  imports: [Paginacion],
  templateUrl: './listado-usuarios.html',
  styleUrl: './listado-usuarios.css',
})
export class ListadoUsuarios {
  private readonly usuarios = signal<readonly Usuario[]>(USUARIOS_DEMO);

  protected readonly termino = signal('');
  protected readonly rol = signal('todos');
  protected readonly estado = signal<FiltroEstado>('todos');
  protected readonly paginaActual = signal(1);

  /** Opciones del selector de rol; se derivan de los datos para no duplicar el catálogo. */
  protected readonly roles = computed(() =>
    [...new Set(this.usuarios().map((usuario) => usuario.rol))].sort((a, b) => a.localeCompare(b)),
  );

  protected readonly filtrados = computed(() => {
    const busqueda = this.termino().trim().toLowerCase();
    const rolElegido = this.rol();
    const estadoElegido = this.estado();

    return this.usuarios().filter((usuario) => {
      const coincideBusqueda =
        !busqueda || `${usuario.nombre} ${usuario.correo}`.toLowerCase().includes(busqueda);
      const coincideRol = rolElegido === 'todos' || usuario.rol === rolElegido;
      const coincideEstado =
        estadoElegido === 'todos' ||
        (estadoElegido === 'activos' ? usuario.activo : !usuario.activo);

      return coincideBusqueda && coincideRol && coincideEstado;
    });
  });

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / TAMANO_PAGINA)),
  );

  /** Índice de la primera fila de la página actual, base para la columna "No.". */
  protected readonly desde = computed(() => (this.paginaActual() - 1) * TAMANO_PAGINA);

  protected readonly usuariosPagina = computed(() =>
    this.filtrados().slice(this.desde(), this.desde() + TAMANO_PAGINA),
  );

  protected buscar(valor: string): void {
    this.termino.set(valor);
    this.reiniciarPagina();
  }

  protected filtrarPorRol(valor: string): void {
    this.rol.set(valor);
    this.reiniciarPagina();
  }

  protected filtrarPorEstado(valor: string): void {
    this.estado.set(valor as FiltroEstado);
    this.reiniciarPagina();
  }

  protected irAPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas()) {
      return;
    }

    this.paginaActual.set(pagina);
  }

  protected agregar(): void {
    // TODO: abrir el formulario de creación de usuario.
  }

  protected editar(usuario: Usuario): void {
    // TODO: abrir el formulario de edición del usuario.
  }

  protected permisos(usuario: Usuario): void {
    // TODO: abrir la pantalla de permisos del usuario.
  }

  /** Al cambiar cualquier filtro la página anterior puede quedar fuera de rango. */
  private reiniciarPagina(): void {
    this.paginaActual.set(1);
  }
}
