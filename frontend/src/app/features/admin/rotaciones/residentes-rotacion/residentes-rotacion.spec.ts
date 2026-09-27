import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ResidentesRotacion } from './residentes-rotacion';
import { RotacionService } from '../../../../services/rotacion.service';
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
});
