import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MapaOcupacion } from './mapa-ocupacion';

describe('MapaOcupacion', () => {
  let component: MapaOcupacion;
  let fixture: ComponentFixture<MapaOcupacion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MapaOcupacion],
    }).compileComponents();

    fixture = TestBed.createComponent(MapaOcupacion);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('datos', {
      meses: ['2025-11', '2025-12'],
      filas: [
        {
          servicio: 'Alto Riesgo',
          cupo: 50,
          diasExceso: 30,
          maximo: 160,
          celdas: [
            { mes: '2025-11', pico: 20, porcentaje: 40 },
            { mes: '2025-12', pico: 80, porcentaje: 160 },
          ],
        },
      ],
    });
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('rotula los meses y marca como exceso solo lo que supera el cupo', () => {
    const mapa: HTMLElement = fixture.nativeElement;
    const meses = [...mapa.querySelectorAll('.mapa__mes')].map((m) => m.textContent?.trim());
    expect(meses).toEqual(['nov 25', 'dic 25']);
    const celdas = mapa.querySelectorAll('.mapa .celda');
    expect(celdas[0].classList).toContain('celda--2');
    expect(celdas[1].classList).toContain('celda--exceso');
    expect(celdas[1].textContent?.trim()).toBe('160%');
  });
});
