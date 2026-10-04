import { TestBed } from '@angular/core/testing';
import { FormularioCoordinador } from './formulario-coordinador';
describe('FormularioCoordinador', () => {
  it('exige los campos y permite un registro completo', async () => {
    const fixture = TestBed.createComponent(FormularioCoordinador);
    fixture.componentRef.setInput('datos', { usuarioId: null, servicioId: null });
    fixture.componentRef.setInput('servicios', [{ id: 1, nombre: 'Servicio' }]);
    fixture.componentRef.setInput('usuarios', [{ id: 2, nombre: 'Usuario' }]);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    fixture.componentRef.setInput('datos', { usuarioId: 2, servicioId: 1 });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
    fixture.componentRef.setInput('guardando', true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
});
