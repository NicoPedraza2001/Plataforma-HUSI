import { Routes } from '@angular/router';
import { Landing } from './features/public/landing/pages/landing/landing';
import { PublicLayout } from './layout/public/public-layout/public-layout';
import { Login } from './features/auth/login/login';

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
  }
];
