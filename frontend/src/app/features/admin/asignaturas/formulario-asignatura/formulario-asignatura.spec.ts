import { TestBed } from '@angular/core/testing';
import { FormularioAsignatura } from './formulario-asignatura';
describe('FormularioAsignatura', () => {
  it('exige los campos y permite un registro completo', async () => {
    const f = TestBed.createComponent(FormularioAsignatura);
    f.componentRef.setInput('datos', { nombre: '', servicioId: null });
    f.componentRef.setInput('servicios', [{ id: 1, nombre: 'Servicio' }]);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    f.componentRef.setInput('datos', { nombre: 'Materia', servicioId: 1 });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
    f.componentRef.setInput('guardando', true);
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
});
