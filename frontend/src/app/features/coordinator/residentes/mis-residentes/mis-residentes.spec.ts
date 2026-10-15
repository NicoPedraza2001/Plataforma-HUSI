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
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(filas.length).toBe(1);
    expect(filas[0].textContent).toContain('Julián Camilo Zapata');
  });

  it('informa cuando no hay residentes rotando hoy', async () => {
    coordinadorService.listarResidentes.mockReturnValue(of({ ...DATOS, residentes: [] }));
    await crear();
    expect(fixture.nativeElement.textContent).toContain('No tienes residentes rotando hoy');
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
