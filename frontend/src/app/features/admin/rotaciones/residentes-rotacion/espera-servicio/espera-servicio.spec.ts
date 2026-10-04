import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RotacionService } from '../../../../../services/rotacion.service';
import { EsperaServicio } from './espera-servicio';

describe('EsperaServicio', () => {
  const destino = {
    asignaturaId: 4,
    asignatura: 'Destino',
    servicio: 'Neuropsicogeriatría',
    servicioId: 3,
    inicio: '2091-03-01',
    fin: '2091-03-31',
    activa: true,
    disponibilidad: 'Disponible',
    cupoDisponible: 2,
    relaciones: 0,
  };
  const espera = (residenteId: number, cruce = false) => ({
    residenteId,
    nombre: 'Residente ' + residenteId,
    documento: 'CC ' + residenteId,
    asignaturaId: 2,
    asignatura: 'Origen',
    inicio: '2091-01-01',
    fin: '2091-01-31',
    cruce,
  });

  async function crear(filas = [espera(1), espera(2, true), espera(3), espera(4)]) {
    const api = {
      consultar: vi.fn(() => of(destino)),
      espera: vi.fn(() => of(filas)),
      trasladarEspera: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({ providers: [{ provide: RotacionService, useValue: api }] });
    const f = TestBed.createComponent(EsperaServicio);
    f.componentRef.setInput('rotacion', destino);
    f.detectChanges();
    await new Promise((r) => setTimeout(r));
    f.detectChanges();
    return { f, api };
  }

  it('should create', async () => {
    const { f } = await crear([]);
    expect(f.componentInstance).toBeTruthy();
    expect(f.nativeElement.querySelector('section')).toBeNull();
  });

  it('preselecciona por llegada hasta el cupo, sin quienes tienen cruce, y traslada', async () => {
    const { f, api } = await crear();
    const marcados = [...f.nativeElement.querySelectorAll('input:checked')].map(
      (i: HTMLInputElement) => i.id,
    );
    expect(marcados).toEqual(['espera-0', 'espera-2']);
    expect(f.nativeElement.textContent).toContain('Tiene otra rotación asignada');
    const emitidos: number[] = [];
    f.componentInstance.trasladados.subscribe((n) => emitidos.push(n));
    await (f.componentInstance as any).trasladar();
    expect(api.trasladarEspera).toHaveBeenCalledWith(destino, [
      { residenteId: 1, asignaturaId: 2, inicio: '2091-01-01', fin: '2091-01-31' },
      { residenteId: 3, asignaturaId: 2, inicio: '2091-01-01', fin: '2091-01-31' },
    ]);
    expect(emitidos).toEqual([2]);
  });

  it('no permite seleccionar más residentes que cupos', async () => {
    const { f, api } = await crear();
    (f.nativeElement.querySelector('#espera-3') as HTMLInputElement).click();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('solo hay 2 cupos disponibles');
    const boton = f.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(boton.disabled).toBe(true);
    await (f.componentInstance as any).trasladar();
    expect(api.trasladarEspera).not.toHaveBeenCalled();
  });
});
