import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { ProgramaRequest } from '../../../../../models/programa.model';
import { UniversidadDetalle } from '../../../../../models/universidad.model';
import { ProgramaService } from '../../../../../services/programa.service';
import { UniversidadService } from '../../../../../services/universidad.service';
import {
  crearFilaDePrueba,
  crearPaginaDePrueba,
  esperarCargas,
  PROGRAMA_MEDICINA_INTERNA,
  PROGRAMA_PEDIATRIA,
  UNIVERSIDAD_ANDES,
  UNIVERSIDAD_BARCELONA,
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

  it('en universidades externas a la Javeriana indica el convenio según el país', () => {
    const filas = [
      crearFilaDePrueba({
        fila: 2,
        universidad: UNIVERSIDAD_ANDES.nombre,
        programa: 'Especialización en Pediatría',
      }),
    ];
    const [grupo] = emparejarProgramas(filas, new Map(), [UNIVERSIDAD_ANDES], []);
    expect(grupo.estado).toBe('nuevo');
    expect(grupo.observaciones[0]).toContain('Movilidad Nacional');
  });

  it('no deja usar el convenio internacional en una universidad nacional', () => {
    const filas = [
      crearFilaDePrueba({
        fila: 2,
        universidad: UNIVERSIDAD_ANDES.nombre,
        programa: 'Convenio Internacional',
      }),
    ];
    const [grupo] = emparejarProgramas(filas, new Map(), [UNIVERSIDAD_ANDES], []);
    expect(grupo.estado).toBe('incompleto');
    expect(grupo.observaciones[0]).toContain('es nacional');
  });

  it('no deja usar el convenio nacional en una universidad del exterior', () => {
    const filas = [
      crearFilaDePrueba({
        fila: 2,
        universidad: UNIVERSIDAD_BARCELONA.nombre,
        pais: UNIVERSIDAD_BARCELONA.pais,
        programa: 'Convenio Nacional',
      }),
    ];
    const [grupo] = emparejarProgramas(filas, new Map(), [UNIVERSIDAD_BARCELONA], []);
    expect(grupo.estado).toBe('incompleto');
    expect(grupo.observaciones[0]).toContain('es internacional');
  });
});

describe('PasoProgramas', () => {
  async function crearPaso(
    filas: FilaCargaInicial[],
    universidades: UniversidadDetalle[] = [UNIVERSIDAD_JAVERIANA],
  ) {
    const programaServiceFalso = {
      buscar: () => of(crearPaginaDePrueba([PROGRAMA_MEDICINA_INTERNA])),
      crear: vi.fn((datos: ProgramaRequest) =>
        of({
          id: 30,
          nombre: datos.nombre,
          universidadId: datos.universidadId,
          universidad: universidades.find((universidad) => universidad.id === datos.universidadId)!
            .nombre,
          estado: 'Activo',
        }),
      ),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: ProgramaService, useValue: programaServiceFalso },
        {
          provide: UniversidadService,
          useValue: { buscar: () => of(crearPaginaDePrueba(universidades)) },
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
    return { paso, filasEmitidas, programaServiceFalso };
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

  it('asigna un solo convenio a todos los programas de una universidad externa', async () => {
    const { paso, filasEmitidas, programaServiceFalso } = await crearPaso(
      [
        crearFilaDePrueba({ fila: 2, universidad: UNIVERSIDAD_ANDES.nombre, programa: 'Cirugía' }),
        crearFilaDePrueba({ fila: 3, universidad: UNIVERSIDAD_ANDES.nombre, programa: 'Urología' }),
      ],
      [UNIVERSIDAD_JAVERIANA, UNIVERSIDAD_ANDES],
    );
    expect(paso.gruposParaCrearEnLote()).toHaveLength(2);
    await paso.crearProgramasEnLote();
    expect(programaServiceFalso.crear).toHaveBeenCalledTimes(1);
    expect(programaServiceFalso.crear).toHaveBeenCalledWith({
      nombre: 'Convenio Nacional',
      universidadId: UNIVERSIDAD_ANDES.id,
    });
    expect(filasEmitidas[0].map((fila) => fila.programa)).toEqual([
      'Convenio Nacional',
      'Convenio Nacional',
    ]);
  });
});
