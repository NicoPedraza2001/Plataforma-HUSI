import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

/** Datos de la cuenta que se muestran y editan en esta pantalla. */
export interface PerfilUsuario {
  nombres: string;
  apellidos: string;
  correo: string;
  contrasena: string;
  rol: string;
}

/** Secciones que se editan de forma independiente. */
export type Seccion = 'perfil' | 'credenciales';

/** Longitud mínima exigida a una contraseña nueva. */
const MINIMO_CONTRASENA = 8;

// TODO: reemplazar por la sesión que entregue el backend.
const PERFIL_DEMO: PerfilUsuario = {
  nombres: 'Gabriela Alejandra',
  apellidos: 'Perez Rodriguez',
  correo: 'gabriela.perez@husi.org.co',
  contrasena: 'HusiAdmin2024',
  rol: 'Administrador',
};

@Component({
  selector: 'app-ajustes',
  imports: [ReactiveFormsModule],
  templateUrl: './ajustes.html',
  styleUrl: './ajustes.css',
})
export class Ajustes {
  private readonly fb = inject(FormBuilder);

  /** Estado ya guardado; es la referencia contra la que se detectan cambios. */
  protected readonly perfil = signal<PerfilUsuario>(PERFIL_DEMO);

  /** Sección abierta en modo edición, o null si todo está en solo lectura. */
  protected readonly enEdicion = signal<Seccion | null>(null);

  /** Sección recién guardada, para mostrar el aviso de confirmación. */
  protected readonly guardada = signal<Seccion | null>(null);

  protected readonly verContrasena = signal(false);

  protected readonly minimoContrasena = MINIMO_CONTRASENA;

  protected readonly formPerfil = this.fb.nonNullable.group({
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    confirmaNombres: [''],
    confirmaApellidos: [''],
  });

  protected readonly formCredenciales = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.minLength(MINIMO_CONTRASENA)]],
    confirmaCorreo: [''],
    confirmaContrasena: [''],
  });

  /** Valores en vivo de cada formulario, para derivar el estado con computed. */
  private readonly valoresPerfil = toSignal(this.formPerfil.valueChanges, {
    initialValue: this.formPerfil.getRawValue(),
  });

  private readonly valoresCredenciales = toSignal(this.formCredenciales.valueChanges, {
    initialValue: this.formCredenciales.getRawValue(),
  });

  constructor() {
    this.restablecerPerfil();
    this.restablecerCredenciales();
  }

  /** Iniciales del avatar: primera letra del nombre y del apellido. */
  protected readonly iniciales = computed(() => {
    const { nombres, apellidos } = this.perfil();
    return `${nombres.charAt(0)}${apellidos.charAt(0)}`.toUpperCase();
  });

  protected readonly nombreCompleto = computed(() => {
    const { nombres, apellidos } = this.perfil();
    return `${nombres} ${apellidos}`;
  });

  /** Máscara de la contraseña guardada mientras no se pida verla. */
  protected readonly contrasenaEnmascarada = computed(() =>
    '•'.repeat(this.perfil().contrasena.length),
  );

  // ==================== Estado derivado ====================
  // Todo lo derivado sale de un único computed por formulario que lee la señal
  // de valores. Es deliberado: la validez de un FormControl no es reactiva, así
  // que si se leyera desde un computed cuya dependencia es un booleano, ese
  // booleano podría no cambiar (sigue habiendo "cambio en el correo") y el
  // resultado se quedaría congelado con la validez anterior. valueChanges emite
  // un objeto nuevo en cada tecla, y eso es lo que fuerza el recálculo.

  private readonly estadoPerfil = computed(() => {
    const valores = this.valoresPerfil();
    const { nombres: guardadoNombres, apellidos: guardadoApellidos } = this.perfil();
    const controles = this.formPerfil.controls;

    const nombres = (valores.nombres ?? '').trim();
    const apellidos = (valores.apellidos ?? '').trim();

    // Solo se pide confirmar lo que de verdad cambió: un campo intacto no
    // estorba con un segundo campo que repetir.
    const cambioNombres = nombres !== guardadoNombres;
    const cambioApellidos = apellidos !== guardadoApellidos;

    const confirmaNombresOk =
      !cambioNombres || (valores.confirmaNombres ?? '').trim() === nombres;
    const confirmaApellidosOk =
      !cambioApellidos || (valores.confirmaApellidos ?? '').trim() === apellidos;

    return {
      cambioNombres,
      cambioApellidos,
      confirmaNombresOk,
      confirmaApellidosOk,
      puedeGuardar:
        (cambioNombres || cambioApellidos) &&
        controles.nombres.valid &&
        controles.apellidos.valid &&
        confirmaNombresOk &&
        confirmaApellidosOk,
    };
  });

  private readonly estadoCredenciales = computed(() => {
    const valores = this.valoresCredenciales();
    const guardadoCorreo = this.perfil().correo;
    const controles = this.formCredenciales.controls;

    const correo = (valores.correo ?? '').trim();
    const contrasena = valores.contrasena ?? '';

    const cambioCorreo = correo !== guardadoCorreo;
    // La contraseña arranca vacía al editar: si trae algo, es que se cambia.
    const cambioContrasena = contrasena !== '';

    const confirmaCorreoOk = !cambioCorreo || (valores.confirmaCorreo ?? '').trim() === correo;
    const confirmaContrasenaOk =
      !cambioContrasena || (valores.confirmaContrasena ?? '') === contrasena;

    return {
      cambioCorreo,
      cambioContrasena,
      confirmaCorreoOk,
      confirmaContrasenaOk,
      correoInvalido: cambioCorreo && controles.correo.invalid,
      contrasenaCorta: cambioContrasena && controles.contrasena.invalid,
      puedeGuardar:
        (cambioCorreo || cambioContrasena) &&
        controles.correo.valid &&
        controles.contrasena.valid &&
        confirmaCorreoOk &&
        confirmaContrasenaOk,
    };
  });

  // Vistas finas para la plantilla: cada una colapsa a un booleano, de modo que
  // el @if solo se re-renderiza cuando ese booleano cambia de verdad.

  protected readonly cambioNombres = computed(() => this.estadoPerfil().cambioNombres);
  protected readonly cambioApellidos = computed(() => this.estadoPerfil().cambioApellidos);
  protected readonly confirmaNombresOk = computed(() => this.estadoPerfil().confirmaNombresOk);
  protected readonly confirmaApellidosOk = computed(() => this.estadoPerfil().confirmaApellidosOk);
  protected readonly puedeGuardarPerfil = computed(() => this.estadoPerfil().puedeGuardar);

  protected readonly cambioCorreo = computed(() => this.estadoCredenciales().cambioCorreo);
  protected readonly cambioContrasena = computed(() => this.estadoCredenciales().cambioContrasena);
  protected readonly confirmaCorreoOk = computed(() => this.estadoCredenciales().confirmaCorreoOk);
  protected readonly confirmaContrasenaOk = computed(
    () => this.estadoCredenciales().confirmaContrasenaOk,
  );
  protected readonly correoInvalido = computed(() => this.estadoCredenciales().correoInvalido);
  protected readonly contrasenaCorta = computed(() => this.estadoCredenciales().contrasenaCorta);
  protected readonly puedeGuardarCredenciales = computed(
    () => this.estadoCredenciales().puedeGuardar,
  );

  // ==================== Acciones ====================

  protected editar(seccion: Seccion): void {
    this.guardada.set(null);
    this.enEdicion.set(seccion);

    if (seccion === 'perfil') {
      this.restablecerPerfil();
    } else {
      this.restablecerCredenciales();
    }
  }

  protected cancelar(seccion: Seccion): void {
    this.enEdicion.set(null);
    this.verContrasena.set(false);

    if (seccion === 'perfil') {
      this.restablecerPerfil();
    } else {
      this.restablecerCredenciales();
    }
  }

  protected guardarPerfil(): void {
    if (!this.puedeGuardarPerfil()) {
      return;
    }

    const { nombres, apellidos } = this.formPerfil.getRawValue();

    this.perfil.update((perfil) => ({
      ...perfil,
      nombres: nombres.trim(),
      apellidos: apellidos.trim(),
    }));

    this.enEdicion.set(null);
    this.guardada.set('perfil');
    this.restablecerPerfil();
  }

  protected guardarCredenciales(): void {
    if (!this.puedeGuardarCredenciales()) {
      return;
    }

    const { correo, contrasena } = this.formCredenciales.getRawValue();

    this.perfil.update((perfil) => ({
      ...perfil,
      correo: correo.trim(),
      // Una contraseña vacía significa "no la toques".
      contrasena: contrasena === '' ? perfil.contrasena : contrasena,
    }));

    this.enEdicion.set(null);
    this.verContrasena.set(false);
    this.guardada.set('credenciales');
    this.restablecerCredenciales();
  }

  protected alternarContrasena(): void {
    this.verContrasena.update((visible) => !visible);
  }

  // ==================== Utilidades de plantilla ====================

  protected editando(seccion: Seccion): boolean {
    return this.enEdicion() === seccion;
  }

  /**
   * Mientras se edita una sección la otra queda en solo lectura, para que no
   * haya dos juegos de cambios a medias compitiendo por guardarse.
   */
  protected bloqueada(seccion: Seccion): boolean {
    const abierta = this.enEdicion();
    return abierta !== null && abierta !== seccion;
  }

  private restablecerPerfil(): void {
    const { nombres, apellidos } = this.perfil();
    this.formPerfil.reset({ nombres, apellidos, confirmaNombres: '', confirmaApellidos: '' });
  }

  private restablecerCredenciales(): void {
    this.formCredenciales.reset({
      correo: this.perfil().correo,
      contrasena: '',
      confirmaCorreo: '',
      confirmaContrasena: '',
    });
  }
}
