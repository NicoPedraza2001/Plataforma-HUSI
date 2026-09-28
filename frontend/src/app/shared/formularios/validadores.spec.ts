import { FormControl } from '@angular/forms';
import { claveBcrypt, enteroBaseDatos, textoNoVacio } from './validadores';
describe('Restricciones de entrada', () => {
  it('rechaza nombres con espacios y acepta nombres con acentos', () => {
    expect(textoNoVacio(new FormControl('   '))).toEqual({ textoVacio: true });
    expect(textoNoVacio(new FormControl('José Pérez'))).toBeNull();
  });
  it('rechaza decimales, negativos, texto e integer fuera de rango', () => {
    for (const valor of [1.5, -1, '1', 2147483648])
      expect(enteroBaseDatos(new FormControl(valor))).not.toBeNull();
    for (const valor of [null, 0, 2147483647])
      expect(enteroBaseDatos(new FormControl(valor))).toBeNull();
  });
  it('mide la contraseña en bytes UTF-8 y permite dejarla vacía al editar', () => {
    expect(claveBcrypt(new FormControl('á'.repeat(37)))).not.toBeNull();
    expect(claveBcrypt(new FormControl('á'.repeat(36)))).toBeNull();
    expect(claveBcrypt(new FormControl(''))).toBeNull();
    expect(claveBcrypt(new FormControl('1234567'))).not.toBeNull();
  });
});
