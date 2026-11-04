import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  ActualizacionCredencialesRequest,
  ActualizacionPerfilRequest,
  Perfil,
} from '../../../models/sesion.model';
import { mensajeError } from '../../../services/api-error';
import { SesionActual } from '../../../services/sesion-actual';
import {
  MOTIVO_CREDENCIALES_ACTUALIZADAS,
  PARAMETRO_MOTIVO,
} from '../../../services/sesion-expirada.interceptor';
import { SesionService } from '../../../services/sesion.service';
import { FocoModal } from '../../../shared/foco-modal/foco-modal';
import { esClaveBcryptValida } from '../../../shared/formularios/validadores';

/** Cada bloque de la pantalla se edita y se guarda por separado. */
type BloqueAjustes = 'perfil' | 'credenciales';

/** Campos de contraseña con su propio botón de «ver / ocultar». */
type CampoContrasena = 'nueva' | 'confirmacion' | 'actual';

/** Una fila del resumen que se muestra antes de confirmar: qué cambia y cómo queda. */
interface CambioAjuste {
  campo: string;
  antes: string;
  despues: string;
}

/** Validación básica del lado del cliente; el backend sigue siendo la autoridad. */
const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-ajustes',
  imports: [FormsModule, NgTemplateOutlet, FocoModal],
  templateUrl: './ajustes.html',
  styleUrl: './ajustes.css',
})
export class Ajustes {
  // Dependencias
  private readonly sesionService = inject(SesionService);
  private readonly sesionActual = inject(SesionActual);
  private readonly router = inject(Router);

  // Estado de la pantalla
  protected readonly perfil = signal<Perfil | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly editando = signal<BloqueAjustes | null>(null);
  protected readonly confirmando = signal<BloqueAjustes | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorConfirmacion = signal('');
  protected readonly exito = signal<{ bloque: BloqueAjustes; mensaje: string } | null>(null);
  private readonly contrasenasVisibles = signal(new Set<CampoContrasena>());

  // Valores del formulario (se copian del perfil al empezar a editar)
  protected readonly nombres = signal('');
  protected readonly apellidos = signal('');
  protected readonly correo = signal('');
  protected readonly contrasenaNueva = signal('');
  protected readonly confirmacionContrasena = signal('');
  protected readonly contrasenaActual = signal('');

  protected readonly iniciales = computed(() => {
    const perfil = this.perfil();
    if (!perfil) return '—';
    return [perfil.nombres, perfil.apellidos]
      .map((texto) => texto.trim().charAt(0).toUpperCase())
      .join('');
  });

  // ===================== Nombres y apellidos =====================
  protected readonly cambiaNombres = computed(
    () => this.nombres().trim() !== (this.perfil()?.nombres ?? ''),
  );
  protected readonly cambiaApellidos = computed(
    () => this.apellidos().trim() !== (this.perfil()?.apellidos ?? ''),
  );

  protected readonly errorPerfil = computed(() => {
    if (!this.nombres().trim()) return 'Los nombres no pueden quedar vacíos.';
    if (!this.apellidos().trim()) return 'Los apellidos no pueden quedar vacíos.';
    return '';
  });

  protected readonly cambiosPerfil = computed<CambioAjuste[]>(() => {
    const perfil = this.perfil();
    if (!perfil) return [];
    const cambios: CambioAjuste[] = [];
    if (this.cambiaNombres())
      cambios.push({ campo: 'Nombres', antes: perfil.nombres, despues: this.nombres().trim() });
    if (this.cambiaApellidos())
      cambios.push({
        campo: 'Apellidos',
        antes: perfil.apellidos,
        despues: this.apellidos().trim(),
      });
    return cambios;
  });

  // ===================== Credenciales =====================
  private readonly correoNormalizado = computed(() => this.correo().trim().toLowerCase());
  protected readonly cambiaCorreo = computed(
    () => this.correoNormalizado() !== (this.perfil()?.correo ?? ''),
  );
  protected readonly cambiaContrasena = computed(() => this.contrasenaNueva() !== '');
  protected readonly contrasenasCoinciden = computed(
    () => this.confirmacionContrasena() === this.contrasenaNueva(),
  );

  protected readonly errorCredenciales = computed(() => {
    if (!CORREO_VALIDO.test(this.correoNormalizado()))
      return 'Escribe un correo electrónico válido.';
    if (this.cambiaContrasena() && !esClaveBcryptValida(this.contrasenaNueva()))
      return 'La contraseña debe tener al menos 8 caracteres y no superar 72 bytes.';
    if (this.cambiaContrasena() && !this.contrasenasCoinciden())
      return 'La confirmación no coincide con la nueva contraseña.';
    return '';
  });

  protected readonly cambiosCredenciales = computed<CambioAjuste[]>(() => {
    const perfil = this.perfil();
    if (!perfil) return [];
    const cambios: CambioAjuste[] = [];
    if (this.cambiaCorreo())
      cambios.push({ campo: 'Correo', antes: perfil.correo, despues: this.correoNormalizado() });
    if (this.cambiaContrasena())
      cambios.push({ campo: 'Contraseña', antes: '••••••••', despues: 'Nueva contraseña' });
    return cambios;
  });

  constructor() {
    this.cargar();
  }

  protected async cargar() {
    this.cargando.set(true);
    this.error.set('');
    try {
      this.perfil.set(await firstValueFrom(this.sesionService.perfil()));
      this.restablecerFormulario();
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected cambiosDe(bloque: BloqueAjustes): CambioAjuste[] {
    return bloque === 'perfil' ? this.cambiosPerfil() : this.cambiosCredenciales();
  }

  protected errorDe(bloque: BloqueAjustes): string {
    return bloque === 'perfil' ? this.errorPerfil() : this.errorCredenciales();
  }

  /** Se puede pedir confirmación cuando hay al menos un cambio y ningún error. */
  protected puedeGuardar(bloque: BloqueAjustes): boolean {
    return this.cambiosDe(bloque).length > 0 && !this.errorDe(bloque);
  }

  protected editar(bloque: BloqueAjustes) {
    this.restablecerFormulario();
    this.exito.set(null);
    this.editando.set(bloque);
  }

  protected cancelarEdicion() {
    this.restablecerFormulario();
    this.editando.set(null);
  }

  protected solicitarConfirmacion(bloque: BloqueAjustes) {
    if (!this.puedeGuardar(bloque)) return;
    this.contrasenaActual.set('');
    this.errorConfirmacion.set('');
    this.confirmando.set(bloque);
  }

  protected cerrarConfirmacion() {
    if (this.guardando()) return;
    this.contrasenaActual.set('');
    this.confirmando.set(null);
  }

  protected async confirmarCambios() {
    const bloque = this.confirmando();
    if (!bloque || this.guardando()) return;
    if (bloque === 'credenciales' && !this.contrasenaActual()) {
      this.errorConfirmacion.set('Escribe tu contraseña actual para confirmar el cambio.');
      return;
    }
    this.guardando.set(true);
    this.errorConfirmacion.set('');
    try {
      if (bloque === 'credenciales') {
        await firstValueFrom(
          this.sesionService.actualizarCredenciales(this.solicitudCredenciales()),
        );
        await this.pedirNuevoIngreso();
        return;
      }
      const perfil = await firstValueFrom(
        this.sesionService.actualizarPerfil(this.solicitudPerfil()),
      );
      this.exito.set({ bloque, mensaje: 'Tus nombres y apellidos se actualizaron.' });
      this.perfil.set(perfil);
      // El nombre del sidebar sale de la sesión compartida: se refresca sin recargar.
      this.sesionActual.reemplazarPerfil(perfil);
      this.confirmando.set(null);
      this.editando.set(null);
      this.restablecerFormulario();
    } catch (e) {
      this.errorConfirmacion.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected esVisible(campo: CampoContrasena): boolean {
    return this.contrasenasVisibles().has(campo);
  }

  protected alternarVisibilidad(campo: CampoContrasena) {
    this.contrasenasVisibles.update((visibles) => {
      const siguientes = new Set(visibles);
      if (siguientes.has(campo)) siguientes.delete(campo);
      else siguientes.add(campo);
      return siguientes;
    });
  }

  /** Solo viaja lo que cambió; el backend conserva lo que se omite. */
  private solicitudPerfil(): ActualizacionPerfilRequest {
    return {
      ...(this.cambiaNombres() && { nombres: this.nombres().trim() }),
      ...(this.cambiaApellidos() && { apellidos: this.apellidos().trim() }),
    };
  }

  private solicitudCredenciales(): ActualizacionCredencialesRequest {
    return {
      contrasenaActual: this.contrasenaActual(),
      ...(this.cambiaCorreo() && { correo: this.correoNormalizado() }),
      ...(this.cambiaContrasena() && { contrasenaNueva: this.contrasenaNueva() }),
    };
  }

  /** El backend ya cerró la sesión: los datos nuevos se estrenan en el login. */
  private async pedirNuevoIngreso() {
    this.sesionActual.olvidar();
    await this.router.navigate(['/login'], {
      queryParams: { [PARAMETRO_MOTIVO]: MOTIVO_CREDENCIALES_ACTUALIZADAS },
    });
  }

  /** Vuelve a mostrar los datos guardados y vacía las contraseñas escritas. */
  private restablecerFormulario() {
    const perfil = this.perfil();
    this.nombres.set(perfil?.nombres ?? '');
    this.apellidos.set(perfil?.apellidos ?? '');
    this.correo.set(perfil?.correo ?? '');
    this.contrasenaNueva.set('');
    this.confirmacionContrasena.set('');
    this.contrasenaActual.set('');
    this.contrasenasVisibles.set(new Set());
  }
}
