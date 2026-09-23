import { TestBed } from '@angular/core/testing';
import { FormularioResidente } from './formulario-residente';
describe('FormularioResidente', () => {
  it('impide guardar mientras falten campos obligatorios', async () => {
    const fixture = TestBed.createComponent(FormularioResidente);
    fixture.componentRef.setInput('datos', {
      nombre: '',
      tipoDocumento: 'CC',
      numeroDocumento: '',
      perfilAcademico: '',
      programaId: null,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });
});
