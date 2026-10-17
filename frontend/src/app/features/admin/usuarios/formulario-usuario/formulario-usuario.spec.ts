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
      servicioId: null,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });

  it('pide el servicio solo cuando el rol es Coordinador', async () => {
    const fixture = TestBed.createComponent(FormularioUsuario);
    const datos = {
      nombre: 'Ana',
      apellido: 'Gómez',
      correo: 'ana@husi.com',
      contrasena: 'Segura-2026',
      rolId: 1,
      servicioId: null,
    };
    fixture.componentRef.setInput('datos', datos);
    fixture.componentRef.setInput('opciones', [
      { id: 1, nombre: 'Administrador' },
      { id: 2, nombre: 'Coordinador' },
    ]);
    fixture.componentRef.setInput('servicios', [{ id: 7, nombre: 'Cirugía' }]);
    await fixture.whenStable();
    const servicio = () => fixture.nativeElement.querySelector('#usuario-servicioId');
    expect(servicio()).toBeNull();

    datos.rolId = 2;
    fixture.componentRef.setInput('datos', { ...datos });
    await fixture.whenStable();
    expect(servicio()).not.toBeNull();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });

  it('al editar no muestra el campo de contraseña', async () => {
    const fixture = TestBed.createComponent(FormularioUsuario);
    fixture.componentRef.setInput('datos', {
      nombre: 'Ana',
      apellido: 'Gómez',
      correo: 'ana@husi.com',
      contrasena: '',
      rolId: 1,
      servicioId: null,
    });
    fixture.componentRef.setInput('edicion', true);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#usuario-contrasena')).toBeNull();
  });

  it('al crear muestra la contraseña sugerida y permite generar otra', async () => {
    const fixture = TestBed.createComponent(FormularioUsuario);
    const datos = {
      nombre: '',
      apellido: '',
      correo: '',
      contrasena: 'Sugerida-2026',
      rolId: null,
      servicioId: null,
    };
    fixture.componentRef.setInput('datos', datos);
    await fixture.whenStable();
    const campo = fixture.nativeElement.querySelector('#usuario-contrasena') as HTMLInputElement;
    expect(campo.value).toBe('Sugerida-2026');

    const botones = [
      ...fixture.nativeElement.querySelectorAll('.clave button'),
    ] as HTMLButtonElement[];
    botones.find((b) => b.textContent?.includes('Generar otra'))!.click();
    expect(datos.contrasena).not.toBe('Sugerida-2026');
    expect(datos.contrasena).toHaveLength(12);
  });
});
