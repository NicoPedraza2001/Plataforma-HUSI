import { Component, inject } from '@angular/core';
import { Perfil } from '../../../models/sesion.model';
import { SesionService } from '../../../services/sesion.service';
import { Consulta } from '../../../shared/consulta/consulta';

@Component({
  selector: 'app-ajustes',
  imports: [],
  templateUrl: './ajustes.html',
  styleUrl: './ajustes.css',
})
export class Ajustes {
  // Dependencias
  private readonly api = inject(SesionService);

  protected readonly consulta = new Consulta<Perfil>(() => this.api.perfil());

  constructor() {
    this.consulta.cargar();
  }
}
