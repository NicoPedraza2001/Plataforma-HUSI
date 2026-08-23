import { Routes } from '@angular/router';
import { Landing } from './features/public/landing/pages/landing/landing';
import { PublicLayout } from './layout/public/public-layout/public-layout';
import { Login } from './features/auth/pages/login/login';

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
        component: Login
      }
    ]
  }
];
