import { TestBed } from '@angular/core/testing';
import { PasoArchivo } from './paso-archivo';

describe('PasoArchivo', () => {
  it('should create', () => {
    const fixture = TestBed.createComponent(PasoArchivo);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('solo permite continuar cuando hay un archivo leído', () => {
    const fixture = TestBed.createComponent(PasoArchivo);
    fixture.detectChanges();
    const botonContinuar = () =>
      [...fixture.nativeElement.querySelectorAll('button')].find((boton: HTMLButtonElement) =>
        boton.textContent?.includes('Continuar'),
      ) as HTMLButtonElement;
    expect(botonContinuar().disabled).toBe(true);
    fixture.componentRef.setInput('libroExcel', { nombreArchivo: 'datos.xlsx', hojas: [] });
    fixture.detectChanges();
    expect(botonContinuar().disabled).toBe(false);
  });
});
