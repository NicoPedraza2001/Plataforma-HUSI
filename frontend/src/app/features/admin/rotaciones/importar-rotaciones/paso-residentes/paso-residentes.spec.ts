import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { ResidenteDetalle, ResidenteRequest } from '../../../../../models/residente.model';
import { PerfilAcademicoService } from '../../../../../services/perfil-academico.service';
import { ProgramaService } from '../../../../../services/programa.service';
import { ResidenteService } from '../../../../../services/residente.service';
import { TipoDocumentoService } from '../../../../../services/tipo-documento.service';
import { UniversidadService } from '../../../../../services/universidad.service';
import {
  crearFilaDePrueba,
  crearPaginaDePrueba,
  esperarCargas,
  PROGRAMA_MEDICINA_INTERNA,
  PROGRAMA_PEDIATRIA,
  UNIVERSIDAD_JAVERIANA,
} from '../datos-de-prueba';
import { PasoResidentes } from './paso-residentes';
import { prepararDatosFormulario, revisarResidentes } from './revisar-residentes';

const TIPOS_DOCUMENTO = [
  { id: 1, codigo: 'CC', nombre: 'Cédula de ciudadanía' },
  { id: 2, codigo: 'PA', nombre: 'Pasaporte' },
];
const PERFILES = [
  { id: 1, nombre: 'Pregrado' },
  { id: 2, nombre: 'Posgrado' },
];
const CATALOGOS = {
  tiposDocumento: TIPOS_DOCUMENTO,
  perfilesAcademicos: PERFILES,
  programas: [PROGRAMA_MEDICINA_INTERNA, PROGRAMA_PEDIATRIA],
};

function crearResidenteRegistrado(cambios: Partial<ResidenteDetalle> = {}): ResidenteDetalle {
  return {
    id: 50,
    nombre: 'Ana Pérez',
    tipoDocumento: 'CC',
    numeroDocumento: '001',
    perfilAcademico: '',
    programaId: PROGRAMA_MEDICINA_INTERNA.id,
    estado: 'Activo',
    ...cambios,
  };
}

describe('revisarResidentes', () => {
  it('reconoce el tipo de documento por nombre y marca como nuevo al no registrado', () => {
    const [revision] = revisarResidentes(
      [crearFilaDePrueba({ tipoDocumento: 'pasaporte' })],
      CATALOGOS,
      new Map(),
    );
    expect(revision.estado).toBe('nuevo');
    expect(revision.codigoTipoDocumento).toBe('PA');
  });

  it('marca incompleto al residente con perfil fuera del catálogo', () => {
    const [revision] = revisarResidentes(
      [crearFilaDePrueba({ perfilAcademico: 'Doctorado' })],
      CATALOGOS,
      new Map(),
    );
    expect(revision.estado).toBe('incompleto');
    expect(revision.observaciones.join(' ')).toContain('Doctorado');
  });

  it('detecta diferencias con el registrado, incluido el perfil que le falta', () => {
    const [revision] = revisarResidentes(
      [crearFilaDePrueba({ perfilAcademico: 'Posgrado', programa: 'Pediatría' })],
      CATALOGOS,
      new Map([['001', crearResidenteRegistrado()]]),
    );
    expect(revision.estado).toBe('diferencias');
    expect(revision.observaciones).toHaveLength(2);
    const datosFormulario = prepararDatosFormulario(revision);
    expect(datosFormulario.perfilAcademico).toBe('Posgrado');
    expect(datosFormulario.programaId).toBe(PROGRAMA_PEDIATRIA.id);
  });

  it('agrupa varias filas del mismo documento y avisa si traen datos distintos', () => {
    const revisiones = revisarResidentes(
      [crearFilaDePrueba({ fila: 2 }), crearFilaDePrueba({ fila: 3, residente: 'Ana M. Pérez' })],
      CATALOGOS,
      new Map(),
    );
    expect(revisiones).toHaveLength(1);
    expect(revisiones[0].numerosFila).toEqual([2, 3]);
    expect(revisiones[0].estado).toBe('incompleto');
  });
});

describe('PasoResidentes', () => {
  async function crearPaso(filas: FilaCargaInicial[], registrados: ResidenteDetalle[] = []) {
    const documentosConsultados: string[][] = [];
    const residenteServiceFalso = {
      buscarPorDocumentos: (documentos: string[]) => {
        documentosConsultados.push(documentos);
        return of(registrados);
      },
      crear: (datos: ResidenteRequest) => of({ id: 77, estado: 'Activo', ...datos }),
      actualizar: (id: number, datos: ResidenteRequest) => of({ id, estado: 'Activo', ...datos }),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: ResidenteService, useValue: residenteServiceFalso },
        { provide: TipoDocumentoService, useValue: { listar: () => of(TIPOS_DOCUMENTO) } },
        { provide: PerfilAcademicoService, useValue: { listar: () => of(PERFILES) } },
        {
          provide: ProgramaService,
          useValue: {
            buscar: () => of(crearPaginaDePrueba([PROGRAMA_MEDICINA_INTERNA, PROGRAMA_PEDIATRIA])),
          },
        },
        {
          provide: UniversidadService,
          useValue: { buscar: () => of(crearPaginaDePrueba([UNIVERSIDAD_JAVERIANA])) },
        },
      ],
    });
    const fixture = TestBed.createComponent(PasoResidentes);
    fixture.componentRef.setInput('filas', filas);
    fixture.detectChanges();
    await esperarCargas();
    fixture.detectChanges();
    const paso = fixture.componentInstance as any;
    const filasEmitidas: FilaCargaInicial[][] = [];
    paso.filasCambiadas.subscribe((filasNuevas: FilaCargaInicial[]) =>
      filasEmitidas.push(filasNuevas),
    );
    return { paso, filasEmitidas, documentosConsultados };
  }

  it('should create', async () => {
    expect((await crearPaso([crearFilaDePrueba()])).paso).toBeTruthy();
  });

  it('consulta los documentos del Excel en un solo bloque, sin repetidos', async () => {
    const { documentosConsultados } = await crearPaso([
      crearFilaDePrueba({ fila: 2, documento: '001' }),
      crearFilaDePrueba({ fila: 3, documento: '001' }),
      crearFilaDePrueba({ fila: 4, documento: '002' }),
    ]);
    expect(documentosConsultados).toEqual([['001', '002']]);
  });

  it('actualiza al residente existente y reescribe sus filas con los datos guardados', async () => {
    const { paso, filasEmitidas } = await crearPaso(
      [crearFilaDePrueba({ perfilAcademico: 'Posgrado' })],
      [crearResidenteRegistrado()],
    );
    const revision = paso.revisiones()[0];
    expect(revision.estado).toBe('diferencias');
    paso.abrirFormulario(revision);
    await paso.guardarResidente();
    expect(filasEmitidas[0][0].perfilAcademico).toBe('Posgrado');
    expect(paso.revisionEnEdicion()).toBeNull();
  });

  it('usar datos del sistema reescribe las filas con lo registrado', async () => {
    const { paso, filasEmitidas } = await crearPaso(
      [crearFilaDePrueba({ residente: 'Ana' })],
      [crearResidenteRegistrado()],
    );
    paso.usarDatosDelSistema(paso.revisiones()[0]);
    expect(filasEmitidas[0][0].residente).toBe('Ana Pérez');
    expect(filasEmitidas[0][0].pais).toBe('Colombia');
  });
});
