import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

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
        { label: 'Programa', link: '/admin/programas' },
        { label: 'Doctores', link: '/admin/doctores' },
        { label: 'Universidad', link: '/admin/universidades' },
        { label: 'Servicios', link: '/admin/servicios' },
      ],
    },
    {
      id: 'administracion',
      title: 'Administracion',
      items: [
        { label: 'Usuarios', link: '/admin/usuarios' },
        { label: 'Historial', link: '/admin/historial' },
        { label: 'Ajustes', link: '/admin/ajustes' },
      ],
    },
  ];
}
