import { ListadoUniversidades } from './features/admin/universidades/listado-universidades/listado-universidades';
import { Routes } from '@angular/router';
import { Landing } from './features/public/landing/pages/landing/landing';
import { PublicLayout } from './layout/public/public-layout/public-layout';
import { Login } from './features/auth/login/login';
import { AdminLayout } from './layout/admin/admin-layout/admin-layout';
import { ListadoProgramas } from './features/admin/programas/listado-programas/listado-programas';
import { ListadoUsuarios } from './features/admin/usuarios/listado-usuarios/listado-usuarios';
import { PanelDashboard } from './features/admin/dashboard/panel-dashboard/panel-dashboard';
import { ListadoServicios } from './features/admin/servicios/listado-servicios/listado-servicios';
import { ListadoResidentes } from './features/admin/residentes/listado-residentes/listado-residentes';
import { Historial } from './features/admin/historial/historial';
import { Ajustes } from './features/admin/ajustes/ajustes';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      {
        path: '',
        component: Landing,
      },
      {
        path: 'login',
        component: Login,
        // Invierte los colores del header y el footer en esta pantalla.
        data: { theme: 'invert' },
      },
    ],
  },
  {
    path: 'admin',
    component: AdminLayout,
    children: [
      { path: '', redirectTo: 'residentes', pathMatch: 'full' },
      { path: 'rotaciones', redirectTo: 'residentes', pathMatch: 'full' },
      { path: 'universidades', component: ListadoUniversidades },
      { path: 'universidades/:universidadId/programas', component: ListadoProgramas },
      {
        path: 'dashboard',
        component: PanelDashboard,
      },
      {
        path: 'programas',
        component: ListadoProgramas,
      },
      {
        path: 'servicios',
        component: ListadoServicios,
      },
      {
        path: 'usuarios',
        component: ListadoUsuarios,
      },
      {
        path: 'historial',
        component: Historial,
      },
      {
        path: 'residentes',
        component: ListadoResidentes,
      },
      {
        path: 'ajustes',
        component: Ajustes,
      },
    ],
  },
];
