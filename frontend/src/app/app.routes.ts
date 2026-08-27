import { Routes } from '@angular/router';
import { Landing } from './features/public/landing/pages/landing/landing';
import { PublicLayout } from './layout/public/public-layout/public-layout';
import { Login } from './features/auth/login/login';
import { AdminLayout } from './layout/admin/admin-layout/admin-layout';
import { ListadoProgramas } from './features/admin/programas/listado-programas/listado-programas';
import { ListadoUsuarios } from './features/admin/usuarios/listado-usuarios/listado-usuarios';
import { PanelDashboard } from './features/admin/dashboard/panel-dashboard/panel-dashboard';
import { ListadoServicios } from './features/admin/servicios/listado-servicios/listado-servicios';
import { Ajustes } from './features/admin/ajustes/ajustes';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      {
        path: '',
        component: Landing
      },
      {
        path: 'login',
        component: Login,
        // Invierte los colores del header y el footer en esta pantalla.
        data: { theme: 'invert' }
      }
    ]
  },
  {
    path: 'admin',
    component: AdminLayout,
    children: [
      {
        path: 'dashboard',
        component: PanelDashboard
      },
      {
        path: 'programas',
        component: ListadoProgramas
      },
      {
        path: 'servicios',
        component: ListadoServicios
      },
      {
        path: 'usuarios',
        component: ListadoUsuarios
      },
      {
        path: 'ajustes',
        component: Ajustes
      }
    ]
  }
];
