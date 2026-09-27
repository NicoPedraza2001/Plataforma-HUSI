import { TestBed } from '@angular/core/testing';
import { FormularioRotacion } from './formulario-rotacion';
describe('FormularioRotacion', () => {
  it('bloquea fechas invertidas y permite fechas válidas', async () => {
    const f = TestBed.createComponent(FormularioRotacion);
    f.componentRef.setInput('asignaturaId', 1);
    f.componentRef.setInput('asignaturas', [
      { id: 1, nombre: 'Materia', servicio: 'Servicio', servicioId: 1 },
    ]);
    f.componentRef.setInput('inicio', '2099-02-01');
    f.componentRef.setInput('fin', '2099-01-01');
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    f.componentRef.setInput('fin', '2099-02-28');
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
  });
});
