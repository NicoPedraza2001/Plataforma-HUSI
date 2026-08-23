import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface LandingFeature {
  icon: 'calendar' | 'user' | 'chart' | 'shield';
  tone: 'green' | 'blue' | 'purple' | 'indigo';
  title: string;
  description: string;
}

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class Landing {
  protected readonly features: LandingFeature[] = [
    {
      icon: 'calendar',
      tone: 'green',
      title: 'Gestión de Rotaciones',
      description: 'Asigna, organiza y supervisa rotaciones de manera eficiente.',
    },
    {
      icon: 'user',
      tone: 'blue',
      title: 'Perfiles Personalizados',
      description: 'Acceso adaptado a diferentes usuarios según su necesidad.',
    },
    {
      icon: 'chart',
      tone: 'purple',
      title: 'Analítica y Reportes',
      description: 'Toma de decisiones informada con datos y reportes inteligentes.',
    },
    {
      icon: 'shield',
      tone: 'indigo',
      title: 'Seguridad y confianza',
      description: 'Información protegida bajo los más altos estándares de seguridad.',
    },
  ];
}
