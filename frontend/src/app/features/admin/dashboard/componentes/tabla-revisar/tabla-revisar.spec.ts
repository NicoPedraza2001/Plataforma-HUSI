import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResidenteRevisar } from '../../../../../models/dashboard.model';
import { TablaRevisar } from './tabla-revisar';

const RESIDENTES: ResidenteRevisar[] = [
  {
    residenteId: 1,
    residente: 'Ana',
    programa: 'Geriatría',
    motivo: 'cruce',
    detalle: ['cruce 1', 'cruce 2', 'cruce 3'].join('\n'),
  },
  { residenteId: 2, residente: 'Luis', programa: 'Geriatría', motivo: 'espera', detalle: 'y' },
];

describe('TablaRevisar', () => {
  let component: TablaRevisar;
  let fixture: ComponentFixture<TablaRevisar>;
  const nombres = () =>
    [...fixture.nativeElement.querySelectorAll('tbody tr')].map((fila: Element) =>
      fila.querySelector('td')?.textContent?.trim(),
    );

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TablaRevisar],
    }).compileComponents();

    fixture = TestBed.createComponent(TablaRevisar);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('residentes', RESIDENTES);
    fixture.componentRef.setInput('periodo', '2025-2');
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('filtra por motivo con los chips, que muestran cuántos hay de cada uno', async () => {
    const chips = [...fixture.nativeElement.querySelectorAll('.chip')] as HTMLButtonElement[];
    expect(chips.map((chip) => chip.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
      'Todos 2',
      'Cruce de fechas 1',
      'En lista de espera 1',
    ]);

    chips[2].click();
    await fixture.whenStable();

    expect(nombres()).toEqual(['Luis']);
    expect(component.motivo()).toBe('espera');
  });

  it('muestra los dos primeros cruces y despliega el resto', async () => {
    const celda = () => fixture.nativeElement.querySelector('tbody .col-detalle');
    const lineas = () =>
      [...celda().querySelectorAll('.detalle__linea')].map((l: Element) => l.textContent?.trim());
    expect(lineas()).toEqual(['cruce 1', 'cruce 2']);
    expect(celda().querySelector('.detalle__mas').textContent.trim()).toBe('+1 más');

    celda().querySelector('.detalle__mas').click();
    await fixture.whenStable();

    expect(lineas()).toEqual(['cruce 1', 'cruce 2', 'cruce 3']);
    expect(celda().querySelector('.detalle__mas').textContent.trim()).toBe('Ver menos');
  });
});
