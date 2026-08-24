import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    usuario: ['', Validators.required],
    contrasena: ['', Validators.required],
  });

  protected readonly showErrors = signal(false);

  protected isInvalid(control: 'usuario' | 'contrasena'): boolean {
    return this.showErrors() && this.form.controls[control].invalid;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.showErrors.set(true);
      return;
    }

    this.showErrors.set(false);
    // TODO: conectar con el servicio de autenticación del backend.
  }
}
