import { generarContrasena } from './generar-contrasena';

describe('generarContrasena', () => {
  it('cumple las reglas del backend e incluye cada tipo de carácter', () => {
    for (let i = 0; i < 50; i++) {
      const clave = generarContrasena();
      expect(clave).toHaveLength(12);
      expect(new TextEncoder().encode(clave).length).toBeLessThanOrEqual(72);
      expect(clave).toMatch(/[A-Z]/);
      expect(clave).toMatch(/[a-z]/);
      expect(clave).toMatch(/[0-9]/);
      expect(clave).toMatch(/[^A-Za-z0-9]/);
    }
  });

  it('no repite la misma contraseña', () => {
    expect(generarContrasena()).not.toBe(generarContrasena());
  });
});
