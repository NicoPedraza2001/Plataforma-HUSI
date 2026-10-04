import { TestBed } from '@angular/core/testing';
import { FormularioServicio } from './formulario-servicio';
describe('FormularioServicio', () => {
  it('bloquea campos incompletos y cupos incoherentes', async () => {
    const fixture = TestBed.createComponent(FormularioServicio);
    fixture.componentRef.setInput('datos', {
      nombre: '',
      descripcion: '',
      cupoSimultaneo: null,
      cupoMaximoInstalado: null,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    fixture.componentRef.setInput('datos', {
      nombre: 'Servicio',
      descripcion: '',
      cupoSimultaneo: 5,
      cupoMaximoInstalado: 2,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });
  it('permite cero y rechaza decimales', async () => {
    const fixture = TestBed.createComponent(FormularioServicio);
    fixture.componentRef.setInput('datos', {
      nombre: 'Servicio',
      descripcion: '',
      cupoSimultaneo: 0,
      cupoMaximoInstalado: 0,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
    fixture.componentRef.setInput('datos', {
      nombre: 'Servicio',
      descripcion: '',
      cupoSimultaneo: 1.5,
      cupoMaximoInstalado: 5,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });
});
