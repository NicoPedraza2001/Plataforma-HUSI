import { TestBed } from '@angular/core/testing';
import { FormularioRotacion } from './formulario-rotacion';
describe('FormularioRotacion', () => {
  it('bloquea fechas invertidas y permite fechas válidas', async () => {
    const fixture = TestBed.createComponent(FormularioRotacion);
    fixture.componentRef.setInput('asignaturaId', 1);
    fixture.componentRef.setInput('asignaturas', [
      { id: 1, nombre: 'Materia', servicio: 'Servicio', servicioId: 1 },
    ]);
    fixture.componentRef.setInput('inicio', '2099-02-01');
    fixture.componentRef.setInput('fin', '2099-01-01');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    fixture.componentRef.setInput('fin', '2099-02-28');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
  });
  it('no permite que una rotación nueva empiece antes de hoy', async () => {
    const fixture = TestBed.createComponent(FormularioRotacion);
    fixture.componentRef.setInput('asignaturaId', 1);
    fixture.componentRef.setInput('inicio', '2020-01-01');
    fixture.componentRef.setInput('fin', '2099-01-01');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('no puede ser anterior a hoy');
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
});
