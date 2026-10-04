import {
  CAMPOS_CARGA,
  detectarColumnas,
  esPlantillaFinal,
  PLANTILLA_FINAL,
  prepararCarga,
  sonConfiguracionesIguales,
} from './campos-carga';

describe('campos de la carga inicial', () => {
  it('detecta encabezados del archivo real y no confunde TIPO con tipo de documento', () => {
    const columnas = detectarColumnas([
      'Name',
      'DOCUMENTO2',
      'Programa',
      'Servicio_HUSI',
      'Rotaciones /asignaturas',
      'Fecha inicial',
      'Fecha final',
      'TIPO',
    ]);
    expect(columnas[3]).toBe(0);
    expect(columnas[4]).toBe(-1);
    expect(columnas[5]).toBe(1);
    expect(columnas[10]).toBe(4);
  });

  it('omite filas vacías aunque haya valores comunes', () => {
    const columnas = CAMPOS_CARGA.map(() => -1);
    columnas[3] = 0;
    expect(
      prepararCarga(
        [['Name'], ['Ana'], ['']],
        1,
        columnas,
        CAMPOS_CARGA.map(() => 'común'),
      ),
    ).toHaveLength(1);
  });

  it('compara configuraciones por contenido', () => {
    const configuracion = {
      indiceHoja: 0,
      filaEncabezados: 1,
      columnaPorCampo: [0, 1],
      valorComunPorCampo: ['', 'CC'],
    };
    expect(sonConfiguracionesIguales(configuracion, { ...configuracion })).toBe(true);
    expect(sonConfiguracionesIguales(configuracion, { ...configuracion, filaEncabezados: 2 })).toBe(
      false,
    );
  });
});

describe('Plantilla institucional definitiva', () => {
  it('reconoce las doce columnas y preserva perfil, documento y campos presentes', () => {
    const columnas = detectarColumnas(PLANTILLA_FINAL);
    const valoresComunes = CAMPOS_CARGA.map(() => '');
    valoresComunes[0] = 'Universidad común';
    valoresComunes[4] = 'CC';
    const filas = prepararCarga(
      [
        PLANTILLA_FINAL,
        [
          'Docencia Servicio',
          'Medicina',
          '00123',
          'Residente ficticio',
          'Posgrado',
          'Universidad específica',
          'Colombia',
          'Asignatura',
          'HUSI_1',
          'Servicio',
          '2098-01-01',
          '2098-01-31',
        ],
        [
          '',
          'Medicina',
          '00456',
          'Otro residente',
          '',
          '',
          '',
          'Asignatura',
          '',
          'Servicio',
          '',
          '',
        ],
        [],
      ],
      1,
      columnas,
      valoresComunes,
    );
    expect(esPlantillaFinal(PLANTILLA_FINAL)).toBe(true);
    expect(filas).toHaveLength(2);
    expect(filas[0].perfilAcademico).toBe('Posgrado');
    expect(filas[0].documento).toBe('00123');
    expect(filas[0].universidad).toBe('Universidad específica');
    expect(filas[1].universidad).toBe('Universidad común');
    expect(filas[1].inicio).toBe('');
  });
});
