import { TestBed } from '@angular/core/testing';
import { crearFilaDePrueba } from '../datos-de-prueba';
import { leerRegistrosNuevos, VistaPreviaCarga } from './vista-previa-carga';

describe('VistaPreviaCarga', () => {
  function crearVistaPrevia() {
    const fixture = TestBed.createComponent(VistaPreviaCarga);
    fixture.componentRef.setInput('filas', [
      crearFilaDePrueba({ fila: 5, residente: 'Primera en prioridad' }),
      crearFilaDePrueba({ fila: 2, residente: 'Con error' }),
      crearFilaDePrueba({ fila: 3, residente: 'Segunda válida' }),
    ]);
    fixture.componentRef.setInput('resultadoValidacion', {
      validacion: 'huella',
      guardado: false,
      total: 3,
      validos: 2,
      espera: 0,
      errores: 1,
      omitidos: 0,
      nuevos: {},
      filas: [
        {
          fila: 5,
          estado: 'VALIDO',
          mensaje:
            'Residente y rotación relacionados con cupo. Se crearán: asignatura, rotacion, rotacion_residente.',
        },
        { fila: 2, estado: 'ERROR', mensaje: 'Falta servicio.' },
        { fila: 3, estado: 'VALIDO', mensaje: 'Residente y rotación relacionados con cupo.' },
      ],
    });
    fixture.detectChanges();
    return { fixture, vistaPrevia: fixture.componentInstance as any };
  }

  it('should create', () => {
    expect(crearVistaPrevia().vistaPrevia).toBeTruthy();
  });

  it('muestra solo las filas válidas, en orden de prioridad y con lo que crean', () => {
    const { vistaPrevia } = crearVistaPrevia();
    const filasAGuardar = vistaPrevia.filasAGuardar();
    expect(filasAGuardar.map((filaAGuardar: any) => filaAGuardar.fila.residente)).toEqual([
      'Primera en prioridad',
      'Segunda válida',
    ]);
    expect(filasAGuardar.map((filaAGuardar: any) => filaAGuardar.prioridad)).toEqual([1, 3]);
    expect(filasAGuardar[0].registrosNuevos).toEqual(['Asignatura', 'Rotación']);
    expect(vistaPrevia.totalFilasQueNoSeImportan()).toBe(1);
  });

  it('lee los registros nuevos del mensaje del backend', () => {
    expect(leerRegistrosNuevos('Correcto. Se crearán: servicio, asignatura.')).toEqual([
      'Servicio',
      'Asignatura',
    ]);
    expect(leerRegistrosNuevos('Residente y rotación relacionados con cupo.')).toEqual([]);
  });

  it('incluye las filas sin cupo marcadas como lista de espera', () => {
    const fixture = TestBed.createComponent(VistaPreviaCarga);
    fixture.componentRef.setInput('filas', [
      crearFilaDePrueba({ fila: 2, residente: 'Con cupo' }),
      crearFilaDePrueba({ fila: 3, residente: 'Sin cupo' }),
    ]);
    fixture.componentRef.setInput('resultadoValidacion', {
      validacion: 'huella',
      guardado: false,
      total: 2,
      validos: 1,
      espera: 1,
      errores: 0,
      omitidos: 0,
      nuevos: {},
      filas: [
        { fila: 2, estado: 'VALIDO', mensaje: 'Residente y rotación relacionados con cupo.' },
        {
          fila: 3,
          estado: 'ESPERA',
          mensaje: 'Sin cupo en esta rotación: quedará en lista de espera (posición 1).',
        },
      ],
    });
    fixture.detectChanges();
    const vistaPrevia = fixture.componentInstance as any;
    expect(vistaPrevia.filasAGuardar().map((f: any) => [f.fila.residente, f.enEspera])).toEqual([
      ['Con cupo', false],
      ['Sin cupo', true],
    ]);
    expect(fixture.nativeElement.textContent).toContain('Lista de espera');
    expect(vistaPrevia.totalFilasQueNoSeImportan()).toBe(0);
  });
});
