import { TestBed } from '@angular/core/testing';
import { crearFilaDePrueba } from '../datos-de-prueba';
import { CorreccionSolicitada, ResultadoCarga } from './resultado-carga';

describe('ResultadoCarga', () => {
  function crearResultado() {
    const fixture = TestBed.createComponent(ResultadoCarga);
    fixture.componentRef.setInput('filas', [crearFilaDePrueba({ fila: 2 })]);
    fixture.componentRef.setInput('resultado', {
      validacion: 'huella',
      guardado: false,
      total: 2,
      validos: 1,
      espera: 0,
      errores: 1,
      omitidos: 0,
      nuevos: {},
      filas: [
        { fila: 2, estado: 'VALIDO', mensaje: 'Correcto' },
        { fila: 3, estado: 'ERROR', mensaje: 'Falta documento.' },
      ],
    });
    fixture.detectChanges();
    return { fixture, resultado: fixture.componentInstance as any };
  }

  it('should create', () => {
    expect(crearResultado().resultado).toBeTruthy();
  });

  it('ofrece corregir el error en el paso que corresponde', () => {
    const { fixture, resultado } = crearResultado();
    let correccion: CorreccionSolicitada | null = null;
    resultado.corregir.subscribe((solicitada: CorreccionSolicitada) => (correccion = solicitada));
    const boton = fixture.nativeElement.querySelector('tbody .btn--outline') as HTMLButtonElement;
    expect(boton.textContent).toContain('Residentes');
    boton.click();
    expect(correccion).toEqual({ numeroFila: 3, paso: 'residentes' });
  });

  it('tras guardar ofrece los no creados (no los que quedaron en espera), incluidas las excluidas', () => {
    const fixture = TestBed.createComponent(ResultadoCarga);
    fixture.componentRef.setInput('filas', [
      crearFilaDePrueba({ fila: 2 }),
      crearFilaDePrueba({ fila: 3 }),
      crearFilaDePrueba({ fila: 5 }),
    ]);
    fixture.componentRef.setInput('filasExcluidas', [crearFilaDePrueba({ fila: 4 })]);
    fixture.componentRef.setInput('terminado', true);
    fixture.componentRef.setInput('resultado', {
      validacion: 'huella',
      guardado: true,
      total: 3,
      validos: 1,
      espera: 1,
      errores: 1,
      omitidos: 0,
      nuevos: {},
      filas: [
        { fila: 2, estado: 'CREADO', mensaje: 'Asignación creada correctamente.' },
        { fila: 3, estado: 'ERROR', mensaje: 'Falta documento.' },
        { fila: 5, estado: 'ESPERA', mensaje: 'Sin cupo: quedó en lista de espera (posición 1).' },
      ],
    });
    fixture.detectChanges();
    const boton = [...fixture.nativeElement.querySelectorAll('button')].find((candidato: any) =>
      candidato.textContent.includes('registros no creados'),
    ) as HTMLButtonElement;
    expect(boton.textContent).toContain('(2)');
    const noCreados = (fixture.componentInstance as any).noCreados();
    expect(noCreados.map((noCreado: any) => [noCreado.fila.fila, noCreado.motivo])).toEqual([
      [3, 'Error'],
      [4, 'Excluida'],
    ]);
  });

  it('antes de guardar no ofrece la descarga de no creados', () => {
    const { fixture } = crearResultado();
    expect(fixture.nativeElement.textContent).not.toContain('registros no creados');
  });
});
