import { TestBed } from '@angular/core/testing';
import { FormularioUsuario } from './formulario-usuario';
describe('FormularioUsuario', () => {
  it('impide guardar mientras falten campos obligatorios', async () => {
    const fixture = TestBed.createComponent(FormularioUsuario);
    fixture.componentRef.setInput('datos', {
      nombre: '',
      apellido: '',
      correo: '',
      contrasena: '',
      rolId: null,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });
});
