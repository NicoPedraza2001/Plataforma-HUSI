import { TestBed } from '@angular/core/testing';
import { FormularioCoordinador } from './formulario-coordinador';
describe('FormularioCoordinador', () => {
  it('exige los campos y permite un registro completo', async () => {
    const f = TestBed.createComponent(FormularioCoordinador);
    f.componentRef.setInput('datos', { usuarioId: null, servicioId: null });
    f.componentRef.setInput('servicios', [{ id: 1, nombre: 'Servicio' }]);
    f.componentRef.setInput('usuarios', [{ id: 2, nombre: 'Usuario' }]);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    f.componentRef.setInput('datos', { usuarioId: 2, servicioId: 1 });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
    f.componentRef.setInput('guardando', true);
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
});
