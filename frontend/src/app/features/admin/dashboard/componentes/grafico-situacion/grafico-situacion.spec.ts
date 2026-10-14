import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GraficoSituacion } from './grafico-situacion';

describe('GraficoSituacion', () => {
  let component: GraficoSituacion;
  let fixture: ComponentFixture<GraficoSituacion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraficoSituacion],
    }).compileComponents();

    fixture = TestBed.createComponent(GraficoSituacion);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('datos', [
      { etiqueta: 'Especialización en Geriatría', conRotacion: 3, enEspera: 1 },
    ]);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('abrevia el programa, suma el total y omite los segmentos vacíos', () => {
    const fila = fixture.nativeElement.querySelector('.fila');
    expect(fila.querySelector('.fila__etiqueta').textContent.trim()).toBe('Geriatría');
    expect(fila.querySelector('.fila__total').textContent.trim()).toBe('4');
    expect(fila.querySelectorAll('.segmento').length).toBe(2);
  });

  it('muestra las mismas cifras en la vista de tabla', async () => {
    fixture.nativeElement.querySelector('.vista').click();
    await fixture.whenStable();
    const celdas = [...fixture.nativeElement.querySelectorAll('tbody td')].map((td: Element) =>
      td.textContent?.trim(),
    );
    expect(celdas).toEqual(['Especialización en Geriatría', '3', '1', '4']);
  });
});
