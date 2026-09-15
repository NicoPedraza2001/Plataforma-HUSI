import { letraColumna } from './excel';

describe('letraColumna', () => {
  it('identifica columnas antes y después de Z', () => {
    expect(letraColumna(0)).toBe('A');
    expect(letraColumna(25)).toBe('Z');
    expect(letraColumna(26)).toBe('AA');
    expect(letraColumna(51)).toBe('AZ');
  });
});
