import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionActual } from '../../../services/sesion-actual';

/** Enlace de la barra lateral del coordinador. */
interface CoordinatorNavItem {
  label: string;
  link: string;
}

/**
 * Shell del panel del coordinador. Comparte la barra lateral del administrador (misma hoja de
 * estilos) para que ambos paneles se vean iguales; solo cambia el menú.
 */
@Component({
  selector: 'app-coordinator-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './coordinator-layout.html',
  styleUrls: ['../../admin/admin-layout/admin-layout.css', './coordinator-layout.css'],
})
export class CoordinatorLayout {
  // Dependencias
  private readonly sesionActual = inject(SesionActual);
  private readonly router = inject(Router);

  protected readonly perfil = this.sesionActual.perfil;

  protected readonly navItems: readonly CoordinatorNavItem[] = [
    { label: 'Mis residentes', link: '/coordinador/residentes' },
    { label: 'Ajustes', link: '/coordinador/ajustes' },
  ];

  protected async cerrarSesion(): Promise<void> {
    // Si el backend no responde, igual se sale: la sesión expira sola en el servidor.
    await this.sesionActual.cerrar().catch(() => undefined);
    await this.router.navigateByUrl('/login');
  }
}
