import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionActual } from '../../../services/sesion-actual';

/** Enlace suelto de la barra lateral. */
interface AdminNavItem {
  label: string;
  link: string;
}

/** Bloque de enlaces; los que llevan título pintan un encabezado sobre la lista. */
interface AdminNavGroup {
  id: string;
  title?: string;
  items: AdminNavItem[];
}

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.css',
})
export class AdminLayout {
  // Dependencias
  private readonly sesionActual = inject(SesionActual);
  private readonly router = inject(Router);

  protected readonly perfil = this.sesionActual.perfil;

  protected async cerrarSesion(): Promise<void> {
    // Si el backend no responde, igual se sale: la sesión expira sola en el servidor.
    await this.sesionActual.cerrar().catch(() => undefined);
    await this.router.navigateByUrl('/login');
  }

  /**
   * Menú único del panel: concentra todas las redirecciones del administrador.
   * Las rutas que todavía no están registradas en app.routes.ts se irán
   * habilitando a medida que se construya cada pantalla.
   */
  protected readonly navGroups: readonly AdminNavGroup[] = [
    {
      id: 'gestion',
      items: [
        { label: 'Rotaciones', link: '/admin/rotaciones' },
        { label: 'Dashboard', link: '/admin/dashboard' },
        { label: 'Residentes', link: '/admin/residentes' },
        { label: 'Universidades', link: '/admin/universidades' },
        { label: 'Programa', link: '/admin/programas' },
        { label: 'Servicios', link: '/admin/servicios' },
        { label: 'Asignaturas', link: '/admin/asignaturas' },
      ],
    },
    {
      id: 'administracion',
      title: 'Administración',
      items: [
        { label: 'Usuarios', link: '/admin/usuarios' },
        { label: 'Coordinadores', link: '/admin/coordinadores' },
        { label: 'Historial', link: '/admin/historial' },
        { label: 'Ajustes', link: '/admin/ajustes' },
      ],
    },
  ];
}
