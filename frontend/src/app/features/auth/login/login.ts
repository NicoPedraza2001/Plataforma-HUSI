import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Perfil } from '../../../models/sesion.model';
import { mensajeError } from '../../../services/api-error';
import { panelDelRol, SesionActual } from '../../../services/sesion-actual';
import {
  MOTIVO_SESION_EXPIRADA,
  PARAMETRO_MOTIVO,
} from '../../../services/sesion-expirada.interceptor';
import { PARAMETRO_VOLVER } from '../../../services/sesion.guard';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  // Dependencias
  private readonly formBuilder = inject(FormBuilder);
  private readonly sesionActual = inject(SesionActual);
  private readonly router = inject(Router);
  private readonly parametros = inject(ActivatedRoute).snapshot.queryParamMap;

  protected readonly formularioLogin = this.formBuilder.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
  });
  protected readonly mostrarErrores = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal(
    this.parametros.get(PARAMETRO_MOTIVO) === MOTIVO_SESION_EXPIRADA
      ? 'Tu sesión expiró. Inicia sesión de nuevo para continuar.'
      : '',
  );

  constructor() {
    this.entrarSiYaHaySesion();
  }

  protected esCampoInvalido(control: 'correo' | 'contrasena'): boolean {
    return this.mostrarErrores() && this.formularioLogin.controls[control].invalid;
  }

  protected async iniciarSesion(): Promise<void> {
    if (this.formularioLogin.invalid) {
      this.mostrarErrores.set(true);
      return;
    }

    this.mostrarErrores.set(false);
    this.error.set('');
    this.enviando.set(true);
    try {
      const perfil = await this.sesionActual.iniciar(this.formularioLogin.getRawValue());
      await this.entrarAlPanel(perfil);
    } catch (e) {
      this.error.set(this.mensajeDeInicioFallido(e));
    } finally {
      // La contraseña no se conserva en el formulario después de enviarla.
      this.formularioLogin.controls.contrasena.reset();
      this.enviando.set(false);
    }
  }

  /** Si el navegador ya tiene una sesión abierta, no pide las credenciales otra vez. */
  private async entrarSiYaHaySesion(): Promise<void> {
    const perfil = await this.sesionActual.consultar().catch(() => null);
    if (perfil && panelDelRol(perfil.rol)) await this.entrarAlPanel(perfil);
  }

  private async entrarAlPanel(perfil: Perfil): Promise<void> {
    const panel = panelDelRol(perfil.rol);
    if (!panel) {
      await this.sesionActual.cerrar().catch(() => undefined);
      this.error.set(`El panel para el perfil ${perfil.rol} aún no está disponible.`);
      return;
    }
    await this.router.navigateByUrl(this.rutaDeRegreso(panel));
  }

  /**
   * Vuelve a la pantalla que pedía sesión, pero solo si está dentro del panel del rol: así un
   * enlace manipulado (`/login?volver=https://otro-sitio`) no puede redirigir fuera de la app.
   */
  private rutaDeRegreso(panel: string): string {
    const volver = this.parametros.get(PARAMETRO_VOLVER);
    const esDelPanel = volver === panel || volver?.startsWith(`${panel}/`);
    return esDelPanel && volver ? volver : panel;
  }

  private mensajeDeInicioFallido(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'Revisa el correo y la contraseña.';
    }
    return mensajeError(error);
  }
}
