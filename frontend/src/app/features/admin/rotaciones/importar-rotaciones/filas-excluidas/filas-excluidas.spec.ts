import { TestBed } from '@angular/core/testing';
import { crearFilaDePrueba } from '../datos-de-prueba';
import { FilaExcluida, FilasExcluidas } from './filas-excluidas';

describe('FilasExcluidas', () => {
  function crear(filas: FilaExcluida[]) {
    const fixture = TestBed.createComponent(FilasExcluidas);
    fixture.componentRef.setInput('filas', filas);
    fixture.detectChanges();
    const restauradas: number[][] = [];
    fixture.componentInstance.restaurar.subscribe((numeros) => restauradas.push(numeros));
    return { fixture, elemento: fixture.nativeElement as HTMLElement, restauradas };
  }

  const EXCLUIDAS: FilaExcluida[] = [
    { fila: crearFilaDePrueba({ fila: 4 }), paso: 'universidades' },
    { fila: crearFilaDePrueba({ fila: 9, residente: 'Luis Gómez' }), paso: 'residentes' },
  ];

  it('should create', () => {
    expect(crear(EXCLUIDAS).fixture.componentInstance).toBeTruthy();
  });

  it('muestra cada fila con el paso donde se excluyó', () => {
    const { elemento } = crear(EXCLUIDAS);
    expect(elemento.querySelector('summary')?.textContent).toContain('2 filas excluidas');
    const filas = elemento.querySelectorAll('tbody tr');
    expect(filas.length).toBe(2);
    expect(filas[1].textContent).toContain('Luis Gómez');
    expect(filas[1].textContent).toContain('Residentes');
  });

  it('restaura una fila o todas', () => {
    const { elemento, restauradas } = crear(EXCLUIDAS);
    (elemento.querySelector('tbody tr button') as HTMLButtonElement).click();
    (elemento.querySelector('.restaurar-todas button') as HTMLButtonElement).click();
    expect(restauradas).toEqual([[4], [4, 9]]);
  });
});
