import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
export interface Perfil { nombres:string; apellidos:string; correo:string; rol:string; }
@Injectable({providedIn:'root'})
export class SesionApi {
 private readonly http=inject(HttpClient);
 perfil() { return this.http.get<Perfil>('/api/sesion/perfil'); }
}
