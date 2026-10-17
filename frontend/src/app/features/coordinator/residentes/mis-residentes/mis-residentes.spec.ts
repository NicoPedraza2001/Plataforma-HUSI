import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ResidenteACargo, ResidentesACargo } from '../../../../models/coordinador.model';
import { Perfil } from '../../../../models/sesion.model';
import { CoordinadorService } from '../../../../services/coordinador.service';
import { SesionActual } from '../../../../services/sesion-actual';
import { MisResidentes } from './mis-residentes';

const PERFIL: Perfil = {
  nombres: 'Gabriela',
  apellidos: 'Pérez',
  correo: 'gabriela.perez@husi.org.co',
  rol: 'Coordinador',
};

function residente(cambios: Partial<ResidenteACargo>): ResidenteACargo {
  return {
    residenteId: 1,
    nombre: 'María José Osorio',
    documento: 'CC 1001',
    universidad: 'Universidad Javeriana',
    programa: 'Medicina interna',
    asignatura: 'Urgencias I',
    inicio: '2026-10-01',
    fin: '2026-10-31',
    ...cambios,
  };
}

const DATOS: ResidentesACargo = {
  servicio: 'Urgencias',
  fecha: '2026-10-14',
  residentes: [
    residente({ residenteId: 1, fin: '2026-10-16' }),
    residente({
      residenteId: 2,
      nombre: 'Julián Camilo Zapata',
      universidad: 'Universidad de los Andes',
      asignatura: 'Urgencias II',
    }),
  ],
  proximos: [
    residente({
      residenteId: 3,
      nombre: 'Laura Restrepo',
      inicio: '2026-10-15',
      fin: '2026-11-15',
    }),
    residente({
      residenteId: 4,
      nombre: 'Andrés Gómez',
      asignatura: 'Urgencias II',
      inicio: '2026-11-01',
      fin: '2026-11-30',
    }),
  ],
};

describe('MisResidentes', () => {
  let component: MisResidentes;
  let fixture: ComponentFixture<MisResidentes>;
  let coordinadorService: { listarResidentes: ReturnType<typeof vi.fn> };

  async function crear() {
    await TestBed.configureTestingModule({
      imports: [MisResidentes],
      providers: [
        { provide: CoordinadorService, useValue: coordinadorService },
        { provide: SesionActual, useValue: { perfil: signal(PERFIL) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MisResidentes);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  }

  /** Tabla de una de las dos tarjetas: 0 = en curso, 1 = próximas rotaciones. */
  function filasDe(tarjeta: number): HTMLTableRowElement[] {
    const card = fixture.nativeElement.querySelectorAll('section.card')[tarjeta];
    return [...card.querySelectorAll('tbody tr')];
  }

  beforeEach(() => {
    coordinadorService = { listarResidentes: vi.fn().mockReturnValue(of(DATOS)) };
  });

  it('should create', async () => {
    await crear();
    expect(component).toBeTruthy();
  });

  it('muestra el servicio, los indicadores y los residentes en curso', async () => {
    await crear();
    const texto: string = fixture.nativeElement.textContent;

    expect(texto).toContain('Gabriela Pérez');
    expect(texto).toContain('Coordinación de Urgencias');
    expect(texto).toContain('María José Osorio');
    expect(texto).toContain('16/10/2026');
    expect(texto).toContain('Faltan 2 días');
    const valores = [...fixture.nativeElement.querySelectorAll('.kpi__valor')].map((kpi) =>
      kpi.textContent.trim(),
    );
    // Residentes, asignaturas, universidades y los que terminan en 7 días o menos.
    expect(valores).toEqual(['2', '2', '2', '1']);
  });

  it('busca sin distinguir tildes ni mayúsculas', async () => {
    await crear();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('.search__input');
    input.value = 'cardiología';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ningún residente coincide');

    input.value = 'JULIAN';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const filas = filasDe(0);
    expect(filas.length).toBe(1);
    expect(filas[0].textContent).toContain('Julián Camilo Zapata');
  });

  it('informa cuando no hay residentes rotando hoy', async () => {
    coordinadorService.listarResidentes.mockReturnValue(
      of({ ...DATOS, residentes: [], proximos: [] }),
    );
    await crear();
    expect(fixture.nativeElement.textContent).toContain('No tienes residentes rotando hoy');
    expect(fixture.nativeElement.textContent).toContain('No hay rotaciones programadas');
  });

  it('lista las próximas rotaciones con los días que faltan para su inicio', async () => {
    await crear();
    const filas = filasDe(1);
    expect(filas.length).toBe(2);
    expect(filas[0].textContent).toContain('Laura Restrepo');
    expect(filas[0].textContent).toContain('15/10/2026');
    expect(filas[0].querySelector('.llegada--pronto')?.textContent).toContain('Empieza mañana');
    expect(filas[1].textContent).toContain('Andrés Gómez');
    expect(filas[1].textContent).toContain('Empieza en 18 días');
    expect(filas[1].querySelector('.llegada--pronto')).toBeNull();
    // Los indicadores siguen contando solo las rotaciones en curso.
    expect(fixture.nativeElement.querySelector('.kpi__valor').textContent.trim()).toBe('2');
  });

  it('busca en las próximas rotaciones sin afectar las rotaciones en curso', async () => {
    await crear();
    const buscador: HTMLInputElement = fixture.nativeElement.querySelectorAll('.search__input')[1];
    buscador.value = 'andres';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(filasDe(1).length).toBe(1);
    expect(filasDe(1)[0].textContent).toContain('Andrés Gómez');
    expect(filasDe(0).length).toBe(2);
  });

  it('sin residentes hoy, anuncia cuándo empieza la próxima rotación', async () => {
    coordinadorService.listarResidentes.mockReturnValue(of({ ...DATOS, residentes: [] }));
    await crear();
    expect(filasDe(0)[0].textContent).toContain('La próxima rotación empieza el 15/10/2026');
  });

  it('muestra el error del backend y permite reintentar', async () => {
    coordinadorService.listarResidentes.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 403,
            error: { mensaje: 'Tu usuario no tiene un servicio asignado.', campos: {} },
          }),
      ),
    );
    await crear();
    const alerta: HTMLElement = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alerta.textContent).toContain('Tu usuario no tiene un servicio asignado.');

    coordinadorService.listarResidentes.mockReturnValue(of(DATOS));
    alerta.querySelector('button')!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('María José Osorio');
  });
});
