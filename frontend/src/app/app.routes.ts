import { Routes } from '@angular/router';
import { Ajustes } from './features/admin/ajustes/ajustes';
import { ListadoAsignaturas } from './features/admin/asignaturas/listado-asignaturas/listado-asignaturas';
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
import { MisResidentes } from './features/coordinator/residentes/mis-residentes/mis-residentes';
import { ROL_ADMINISTRADOR, ROL_COORDINADOR } from './models/sesion.model';
import { Landing } from './features/public/landing/pages/landing/landing';
import { AdminLayout } from './layout/admin/admin-layout/admin-layout';
import { CoordinatorLayout } from './layout/coordinator/coordinator-layout/coordinator-layout';
import { PublicLayout } from './layout/public/public-layout/public-layout';
import { requiereRol } from './services/sesion.guard';

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
    // Todo el panel exige sesión con rol Administrador (el backend lo vuelve a comprobar).
    canActivate: [requiereRol(ROL_ADMINISTRADOR)],
    children: [
      { path: '', redirectTo: 'residentes', pathMatch: 'full' },
      { path: 'rotaciones', component: ListadoRotaciones },
      { path: 'importar-excel', component: ImportarRotaciones },
      { path: 'asignaturas', component: ListadoAsignaturas },
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
  {
    path: 'coordinador',
    component: CoordinatorLayout,
    // Panel del coordinador: solo ve los residentes de su servicio (el backend lo filtra por sesión).
    canActivate: [requiereRol(ROL_COORDINADOR)],
    children: [
      { path: '', redirectTo: 'residentes', pathMatch: 'full' },
      { path: 'residentes', component: MisResidentes },
      // Ajustes de la cuenta propia: misma pantalla del administrador, solo usa /sesion.
      { path: 'ajustes', component: Ajustes },
    ],
  },
];
