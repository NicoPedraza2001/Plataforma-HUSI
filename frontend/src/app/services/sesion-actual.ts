import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  InicioSesionRequest,
  Perfil,
  ROL_ADMINISTRADOR,
  ROL_COORDINADOR,
} from '../models/sesion.model';
import { SesionService } from './sesion.service';

/** Pantalla de inicio de cada rol. Un rol sin panel construido todavía no tiene entrada. */
const PANEL_POR_ROL: Readonly<Record<string, string>> = {
  [ROL_ADMINISTRADOR]: '/admin',
  [ROL_COORDINADOR]: '/coordinador',
};

/** Ruta del panel que corresponde al rol, o `null` si ese rol aún no tiene panel. */
export function panelDelRol(rol: string): string | null {
  return PANEL_POR_ROL[rol] ?? null;
}

/**
 * Usuario que tiene la sesión abierta en este navegador, compartido por el guard, el login y el
 * layout. Es solo una copia para pintar la interfaz: quien decide el acceso es el backend, que
 * revisa la cookie de sesión y el rol en cada petición.
 */
@Injectable({ providedIn: 'root' })
export class SesionActual {
  private readonly sesionService = inject(SesionService);
  private readonly perfilActual = signal<Perfil | null>(null);
  private consultada = false;

  readonly perfil = this.perfilActual.asReadonly();

  /**
   * Pregunta al backend quién tiene la sesión; solo la primera vez, luego usa lo guardado.
   * Devuelve `null` si no hay sesión. Esta consulta también entrega la cookie `XSRF-TOKEN`
   * que el backend exige para iniciar sesión.
   */
  async consultar(): Promise<Perfil | null> {
    if (this.consultada) return this.perfilActual();
    try {
      this.perfilActual.set(await firstValueFrom(this.sesionService.perfil()));
    } catch (error) {
      const sinSesion = error instanceof HttpErrorResponse && error.status === 401;
      if (!sinSesion) throw error;
      this.perfilActual.set(null);
    }
    this.consultada = true;
    return this.perfilActual();
  }

  async iniciar(credenciales: InicioSesionRequest): Promise<Perfil> {
    // Garantiza el token CSRF aunque la consulta inicial haya fallado (p. ej. backend apagado).
    if (!this.consultada) await this.consultar().catch(() => null);
    const perfil = await firstValueFrom(this.sesionService.iniciarSesion(credenciales));
    this.perfilActual.set(perfil);
    this.consultada = true;
    return perfil;
  }

  async cerrar(): Promise<void> {
    try {
      await firstValueFrom(this.sesionService.cerrarSesion());
    } finally {
      // Aunque el backend no responda, la interfaz deja de mostrar la sesión.
      this.olvidar();
    }
  }

  /** Ajustes guardó cambios en la cuenta: el layout muestra los datos nuevos sin recargar. */
  reemplazarPerfil(perfil: Perfil): void {
    this.perfilActual.set(perfil);
  }

  /** El backend avisó que la sesión ya no existe (expiró o se cerró). */
  olvidar(): void {
    this.perfilActual.set(null);
    this.consultada = true;
  }
}
