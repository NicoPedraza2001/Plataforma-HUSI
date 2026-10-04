import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  // Dependencias
  private readonly formBuilder = inject(FormBuilder);

  protected readonly formularioLogin = this.formBuilder.nonNullable.group({
    usuario: ['', Validators.required],
    contrasena: ['', Validators.required],
  });
  protected readonly mostrarErrores = signal(false);

  protected esCampoInvalido(control: 'usuario' | 'contrasena'): boolean {
    return this.mostrarErrores() && this.formularioLogin.controls[control].invalid;
  }

  protected iniciarSesion(): void {
    if (this.formularioLogin.invalid) {
      this.mostrarErrores.set(true);
      return;
    }

    this.mostrarErrores.set(false);
    // TODO: conectar con el servicio de autenticación del backend.
  }
}
