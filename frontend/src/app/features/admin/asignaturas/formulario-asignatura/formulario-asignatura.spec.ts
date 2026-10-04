import { TestBed } from '@angular/core/testing';
import { FormularioAsignatura } from './formulario-asignatura';
describe('FormularioAsignatura', () => {
  it('exige los campos y permite un registro completo', async () => {
    const fixture = TestBed.createComponent(FormularioAsignatura);
    fixture.componentRef.setInput('datos', { nombre: '', servicioId: null });
    fixture.componentRef.setInput('servicios', [{ id: 1, nombre: 'Servicio' }]);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    fixture.componentRef.setInput('datos', { nombre: 'Materia', servicioId: 1 });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
    fixture.componentRef.setInput('guardando', true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
});
