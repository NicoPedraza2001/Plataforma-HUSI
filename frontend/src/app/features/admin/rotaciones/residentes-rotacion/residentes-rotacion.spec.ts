import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RotacionService } from '../../../../services/rotacion.service';
import { ResidentesRotacion } from './residentes-rotacion';
describe('ResidentesRotacion', () => {
  it('permite consultar una rotación inactiva sin ofrecer nuevas asignaciones', async () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: RotacionService,
          useValue: {
            residentes: () =>
              of({ contenido: [], pagina: 1, totalPaginas: 1, totalElementos: 0, tamano: 10 }),
            candidatos: () => of([]),
          },
        },
      ],
    });
    const f = TestBed.createComponent(ResidentesRotacion);
    f.componentRef.setInput('rotacion', {
      asignaturaId: 1,
      asignatura: 'Materia',
      servicio: 'Servicio',
      inicio: '2099-01-01',
      fin: '2099-01-31',
      activa: false,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('La rotación está inactiva');
    expect(f.nativeElement.querySelector('form')).toBeNull();
    expect(f.nativeElement.textContent).toContain('todavía no tiene residentes');
  });
  it('una rotación finalizada se consulta sin acciones', async () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: RotacionService,
          useValue: {
            residentes: () =>
              of({
                contenido: [
                  {
                    residenteId: 1,
                    nombre: 'Residente',
                    documento: 'CC 1',
                    programa: 'Programa',
                    estado: 'Asignado',
                  },
                ],
                pagina: 1,
                totalPaginas: 1,
                totalElementos: 1,
                tamano: 10,
              }),
            candidatos: () => of([]),
          },
        },
      ],
    });
    const f = TestBed.createComponent(ResidentesRotacion);
    f.componentRef.setInput('rotacion', {
      asignaturaId: 1,
      asignatura: 'Materia',
      servicio: 'Servicio',
      inicio: '2020-01-01',
      fin: '2020-01-31',
      activa: true,
      finalizada: true,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('finalizó el 31/01/2020');
    expect(f.nativeElement.querySelector('form')).toBeNull();
    expect(f.nativeElement.querySelector('app-espera-servicio')).toBeNull();
    expect(f.nativeElement.textContent).not.toContain('Cancelar asignación');
  });

  it('muestra aparte a los residentes en espera, con su posición', async () => {
    const pagina = (contenido: any[]) =>
      of({ contenido, pagina: 1, totalPaginas: 1, totalElementos: contenido.length, tamano: 10 });
    const fila = (residenteId: number, nombre: string, estado: string) => ({
      residenteId,
      nombre,
      documento: 'CC ' + residenteId,
      programa: 'Programa',
      estado,
    });
    const residentes = vi.fn((_r: unknown, _p: number, espera: boolean) =>
      espera
        ? pagina([
            fila(3, 'Primera en espera', 'Esperando cupo'),
            fila(4, 'Segunda en espera', 'Esperando cupo'),
          ])
        : pagina([fila(1, 'Asignada', 'Asignado')]),
    );
    TestBed.configureTestingModule({
      providers: [{ provide: RotacionService, useValue: { residentes, candidatos: () => of([]) } }],
    });
    const f = TestBed.createComponent(ResidentesRotacion);
    f.componentRef.setInput('rotacion', {
      asignaturaId: 1,
      asignatura: 'Materia',
      servicio: 'Servicio',
      inicio: '2099-01-01',
      fin: '2099-01-31',
      activa: false,
      finalizada: false,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    const tablas = f.nativeElement.querySelectorAll('table');
    expect(tablas.length).toBe(2);
    expect(tablas[0].textContent).toContain('Asignada');
    expect(tablas[0].textContent).not.toContain('en espera');
    expect(f.nativeElement.textContent).toContain('Residentes en espera');
    const filasEspera = [...tablas[1].querySelectorAll('tbody tr')].map((tr: any) =>
      tr.textContent.replace(/\s+/g, ' ').trim(),
    );
    expect(filasEspera[0]).toMatch(/^1 Primera en espera/);
    expect(filasEspera[1]).toMatch(/^2 Segunda en espera/);
    expect(residentes).toHaveBeenCalledWith(expect.anything(), 1, true);
  });

  it('quita el cupo a un asignado y elimina con confirmación a quien está en espera', async () => {
    const pagina = (contenido: any[]) =>
      of({ contenido, pagina: 1, totalPaginas: 1, totalElementos: contenido.length, tamano: 10 });
    const fila = (residenteId: number, nombre: string, estado: string) => ({
      residenteId,
      nombre,
      documento: 'CC ' + residenteId,
      programa: 'Programa',
      estado,
    });
    const api = {
      residentes: vi.fn((_r: unknown, _p: number, espera: boolean) =>
        espera
          ? pagina([fila(3, 'En espera', 'Esperando cupo')])
          : pagina([fila(1, 'Asignada', 'Asignado')]),
      ),
      candidatos: () => of([]),
      consultar: () => of({ cupoDisponible: 0 }),
      espera: () => of([]),
      estadoResidente: vi.fn(() => of(undefined)),
      eliminarResidente: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({ providers: [{ provide: RotacionService, useValue: api }] });
    const f = TestBed.createComponent(ResidentesRotacion);
    f.componentRef.setInput('rotacion', {
      asignaturaId: 1,
      asignatura: 'Materia',
      servicio: 'Servicio',
      inicio: '2099-01-01',
      fin: '2099-01-31',
      activa: true,
      finalizada: false,
    });
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    const boton = (texto: string) =>
      [...f.nativeElement.querySelectorAll('button')].find(
        (b: any) => b.textContent.trim() === texto,
      ) as HTMLButtonElement;
    expect(f.nativeElement.textContent).not.toContain('Cancelar asignación');
    boton('Quitar cupo').click();
    await f.whenStable();
    expect(api.estadoResidente).toHaveBeenCalledWith(expect.anything(), 1, 'Esperando cupo');
    f.detectChanges();
    boton('Eliminar').click();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('¿Eliminar a este residente de la rotación?');
    expect(api.eliminarResidente).not.toHaveBeenCalled();
    const confirmar = [...f.nativeElement.querySelectorAll('.confirmacion button')].find(
      (b: any) => b.textContent.trim() === 'Eliminar',
    ) as HTMLButtonElement;
    confirmar.click();
    await f.whenStable();
    expect(api.eliminarResidente).toHaveBeenCalledWith(expect.anything(), 3);
  });
});
