import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Restricciones de entrada compartidas; el backend sigue siendo la autoridad. */
export function textoNoVacio(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim().length === 0
    ? { textoVacio: true }
    : null;
}

export function enteroBaseDatos(control: AbstractControl): ValidationErrors | null {
  const valor: unknown = control.value;
  if (valor === null || valor === undefined || valor === '') return null;
  return typeof valor === 'number' && Number.isInteger(valor) && valor >= 0 && valor <= 2147483647
    ? null
    : { enteroBaseDatos: true };
}

/** Misma regla que el backend: al menos 8 caracteres, no solo espacios y hasta 72 bytes. */
export function esClaveBcryptValida(valor: string): boolean {
  return (
    valor.trim().length > 0 && valor.length >= 8 && new TextEncoder().encode(valor).length <= 72
  );
}

export function claveBcrypt(control: AbstractControl): ValidationErrors | null {
  const valor: unknown = control.value;
  if (valor === null || valor === undefined || valor === '') return null;
  return typeof valor === 'string' && esClaveBcryptValida(valor) ? null : { claveBcrypt: true };
}
