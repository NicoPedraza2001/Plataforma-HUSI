import {
  buscarConvenio,
  buscarErrorDeConvenioSegunPais,
  convenioSegunPais,
  esUniversidadPropia,
} from './programas-de-convenio';

describe('programas de convenio', () => {
  it('reconoce a la Javeriana sin importar mayúsculas ni espacios', () => {
    expect(esUniversidadPropia('  pontificia universidad javeriana ')).toBe(true);
    expect(esUniversidadPropia('Universidad del Rosario')).toBe(false);
  });

  it('elige el convenio según el país de la universidad', () => {
    expect(convenioSegunPais('Colombia')).toBe('Convenio Nacional');
    expect(convenioSegunPais(' COLOMBIA ')).toBe('Convenio Nacional');
    expect(convenioSegunPais('México')).toBe('Convenio Internacional');
  });

  it('rechaza el convenio internacional en universidades nacionales', () => {
    const universidadNacional = { nombre: 'Universidad del Rosario', pais: 'colombia' };
    expect(buscarErrorDeConvenioSegunPais('Convenio Internacional', universidadNacional)).toContain(
      'es nacional',
    );
    expect(buscarErrorDeConvenioSegunPais('Convenio Nacional', universidadNacional)).toBeNull();
    expect(
      buscarErrorDeConvenioSegunPais('Convenio Internacional', {
        nombre: 'UBA',
        pais: 'Argentina',
      }),
    ).toBeNull();
  });

  it('rechaza el convenio nacional en universidades del exterior', () => {
    const universidadInternacional = { nombre: 'Universidad de Barcelona', pais: 'españa' };
    expect(buscarErrorDeConvenioSegunPais('Convenio Nacional', universidadInternacional)).toContain(
      'es internacional',
    );
    expect(
      buscarErrorDeConvenioSegunPais('Convenio Internacional', universidadInternacional),
    ).toBeNull();
  });

  it('sin país conocido no bloquea: decide el backend', () => {
    expect(buscarErrorDeConvenioSegunPais('Convenio Nacional', { nombre: 'X' })).toBeNull();
  });

  it('devuelve el convenio con su ortografía exacta o null', () => {
    expect(buscarConvenio(' convenio internacional ')).toBe('Convenio Internacional');
    expect(buscarConvenio('Especialización en Pediatría')).toBeNull();
  });
});
