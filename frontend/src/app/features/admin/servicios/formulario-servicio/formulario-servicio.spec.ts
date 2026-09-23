import { TestBed } from '@angular/core/testing';
import { FormularioServicio } from './formulario-servicio';
describe('FormularioServicio', () => {
  it('bloquea campos incompletos y cupos incoherentes', async () => {
    const f = TestBed.createComponent(FormularioServicio);
    f.componentRef.setInput('datos', {
      nombre: '',
      descripcion: '',
      cupoSimultaneo: null,
      cupoMaximoInstalado: null,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    f.componentRef.setInput('datos', {
      nombre: 'Servicio',
      descripcion: '',
      cupoSimultaneo: 5,
      cupoMaximoInstalado: 2,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });
  it('permite cero y rechaza decimales', async () => {
    const f = TestBed.createComponent(FormularioServicio);
    f.componentRef.setInput('datos', {
      nombre: 'Servicio',
      descripcion: '',
      cupoSimultaneo: 0,
      cupoMaximoInstalado: 0,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
    f.componentRef.setInput('datos', {
      nombre: 'Servicio',
      descripcion: '',
      cupoSimultaneo: 1.5,
      cupoMaximoInstalado: 5,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });
});
