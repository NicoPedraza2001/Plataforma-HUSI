import { Routes } from '@angular/router';
import { Landing } from './features/public/landing/pages/landing/landing';
import { PublicLayout } from './layout/public/public-layout/public-layout';
import { Login } from './features/auth/login/login';
import { AdminLayout } from './layout/admin/admin-layout/admin-layout';
import { ListadoProgramas } from './features/admin/programas/listado-programas/listado-programas';
import { Historial } from './features/admin/historial/historial/historial';

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
        path: 'programas',
        component: ListadoProgramas
      },
      {
        path: 'historial',
        component: Historial
      }
    ]
  }
];
