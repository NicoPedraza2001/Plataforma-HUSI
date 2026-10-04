import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { ProgramaRequest } from '../../../../../models/programa.model';
import { ProgramaService } from '../../../../../services/programa.service';
import { UniversidadService } from '../../../../../services/universidad.service';
import {
  crearFilaDePrueba,
  crearPaginaDePrueba,
  esperarCargas,
  PROGRAMA_MEDICINA_INTERNA,
  PROGRAMA_PEDIATRIA,
  UNIVERSIDAD_JAVERIANA,
} from '../datos-de-prueba';
import { emparejarProgramas } from './emparejar-programas';
import { PasoProgramas } from './paso-programas';

describe('emparejarProgramas', () => {
  it('busca el programa dentro de su universidad y sugiere los parecidos', () => {
    const filas = [
      crearFilaDePrueba({ fila: 2 }),
      crearFilaDePrueba({ fila: 3, programa: 'Pediatria' }),
      crearFilaDePrueba({ fila: 4, universidad: 'Universidad sin registrar' }),
    ];
    const grupos = emparejarProgramas(
      filas,
      new Map(),
      [UNIVERSIDAD_JAVERIANA],
      [PROGRAMA_MEDICINA_INTERNA, PROGRAMA_PEDIATRIA],
    );
    expect(grupos.map((grupo) => grupo.estado)).toEqual(['encontrado', 'nuevo', 'incompleto']);
    expect(grupos[1].sugerencias[0].id).toBe(PROGRAMA_PEDIATRIA.id);
    expect(grupos[2].sePuedeCrear).toBe(false);
  });
});

describe('PasoProgramas', () => {
  async function crearPaso(filas: FilaCargaInicial[]) {
    const programaServiceFalso = {
      buscar: () => of(crearPaginaDePrueba([PROGRAMA_MEDICINA_INTERNA])),
      crear: (datos: ProgramaRequest) =>
        of({
          id: 30,
          nombre: datos.nombre,
          universidadId: datos.universidadId,
          universidad: UNIVERSIDAD_JAVERIANA.nombre,
          estado: 'Activo',
        }),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: ProgramaService, useValue: programaServiceFalso },
        {
          provide: UniversidadService,
          useValue: { buscar: () => of(crearPaginaDePrueba([UNIVERSIDAD_JAVERIANA])) },
        },
      ],
    });
    const fixture = TestBed.createComponent(PasoProgramas);
    fixture.componentRef.setInput('filas', filas);
    fixture.componentRef.setInput('filasOriginales', filas);
    fixture.detectChanges();
    await esperarCargas();
    fixture.detectChanges();
    const paso = fixture.componentInstance as any;
    const filasEmitidas: FilaCargaInicial[][] = [];
    paso.filasCambiadas.subscribe((filasNuevas: FilaCargaInicial[]) =>
      filasEmitidas.push(filasNuevas),
    );
    return { paso, filasEmitidas };
  }

  it('should create', async () => {
    expect((await crearPaso([crearFilaDePrueba()])).paso).toBeTruthy();
  });

  it('crea en lote los programas nuevos sin sugerencias', async () => {
    const { paso, filasEmitidas } = await crearPaso([
      crearFilaDePrueba({ fila: 2, programa: 'Neurocirugía' }),
    ]);
    expect(paso.gruposParaCrearEnLote()).toHaveLength(1);
    await paso.crearProgramasEnLote();
    expect(filasEmitidas[0][0].programa).toBe('Neurocirugía');
    expect(paso.programasDelSistema().map((programa: { id: number }) => programa.id)).toContain(30);
  });
});
