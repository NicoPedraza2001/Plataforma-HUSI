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

export function claveBcrypt(control: AbstractControl): ValidationErrors | null {
  const valor: unknown = control.value;
  if (valor === null || valor === undefined || valor === '') return null;
  return typeof valor === 'string' &&
    valor.trim().length > 0 &&
    valor.length >= 8 &&
    new TextEncoder().encode(valor).length <= 72
    ? null
    : { claveBcrypt: true };
}
