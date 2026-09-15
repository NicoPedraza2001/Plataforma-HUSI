import { Component, inject } from '@angular/core';
import { Consulta } from '../../../services/consulta';
import { SesionApi, Perfil } from '../../../services/sesion-api';
@Component({selector:'app-ajustes',imports:[],templateUrl:'./ajustes.html',styleUrl:'./ajustes.css'})
export class Ajustes {
 private readonly api=inject(SesionApi);
 protected readonly consulta=new Consulta<Perfil>(()=>this.api.perfil());
 constructor() { this.consulta.cargar(); }
}
