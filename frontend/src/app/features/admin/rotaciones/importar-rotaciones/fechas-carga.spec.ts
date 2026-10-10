import { describirErrorDeFechas, normalizarFecha } from './fechas-carga';

describe('normalizarFecha', () => {
  it('convierte los formatos comunes a AAAA-MM-DD', () => {
    expect(normalizarFecha('2026-11-01')).toBe('2026-11-01');
    expect(normalizarFecha('2026/11/1')).toBe('2026-11-01');
    expect(normalizarFecha('01/11/2026')).toBe('2026-11-01');
    expect(normalizarFecha(' 1-11-2026 ')).toBe('2026-11-01');
  });

  it('rechaza fechas que no existen en el calendario', () => {
    expect(normalizarFecha('31/11/2026')).toBeNull();
    expect(normalizarFecha('2026-02-29')).toBeNull();
    expect(normalizarFecha('2028-02-29')).toBe('2028-02-29');
  });

  it('rechaza textos que no son fechas y años fuera de rango', () => {
    expect(normalizarFecha('noviembre')).toBeNull();
    expect(normalizarFecha('')).toBeNull();
    expect(normalizarFecha('01/01/1850')).toBeNull();
  });
});

describe('describirErrorDeFechas', () => {
  it('explica cuál fecha falla', () => {
    expect(describirErrorDeFechas('31/11/2026', '2026-12-30')).toContain('«31/11/2026»');
    expect(describirErrorDeFechas('2026-12-01', '2026-11-30')).toContain('anterior');
    expect(describirErrorDeFechas('', '2026-11-30')).toContain('Indica');
    expect(describirErrorDeFechas('2026-11-01', '30/11/2026')).toBe('');
  });
});
