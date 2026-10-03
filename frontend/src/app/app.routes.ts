import { Routes } from '@angular/router';
import { Ajustes } from './features/admin/ajustes/ajustes';
import { ListadoAsignaturas } from './features/admin/asignaturas/listado-asignaturas/listado-asignaturas';
import { ListadoCoordinadores } from './features/admin/coordinadores/listado-coordinadores/listado-coordinadores';
import { PanelDashboard } from './features/admin/dashboard/panel-dashboard/panel-dashboard';
import { Historial } from './features/admin/historial/historial';
import { ListadoProgramas } from './features/admin/programas/listado-programas/listado-programas';
import { ListadoResidentes } from './features/admin/residentes/listado-residentes/listado-residentes';
import { ImportarRotaciones } from './features/admin/rotaciones/importar-rotaciones/importar-rotaciones';
import { ListadoRotaciones } from './features/admin/rotaciones/listado-rotaciones/listado-rotaciones';
import { ListadoServicios } from './features/admin/servicios/listado-servicios/listado-servicios';
import { ListadoUniversidades } from './features/admin/universidades/listado-universidades/listado-universidades';
import { ListadoUsuarios } from './features/admin/usuarios/listado-usuarios/listado-usuarios';
import { Login } from './features/auth/login/login';
import { Landing } from './features/public/landing/pages/landing/landing';
import { AdminLayout } from './layout/admin/admin-layout/admin-layout';
import { PublicLayout } from './layout/public/public-layout/public-layout';

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
      { path: 'rotaciones', component: ListadoRotaciones },
      { path: 'importar-excel', component: ImportarRotaciones },
      { path: 'asignaturas', component: ListadoAsignaturas },
      { path: 'coordinadores', component: ListadoCoordinadores },
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
