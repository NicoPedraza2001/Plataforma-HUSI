import { claseBadgeAccion, compararDatos } from './comparar-datos';

describe('compararDatos', () => {
  it('marca solo los campos que cambiaron al actualizar', () => {
    const campos = compararDatos(
      { Nombre: 'Javeriana', País: 'Colombia', Estado: 'Activo' },
      { Nombre: 'Javeriana', País: 'Perú', Estado: 'Activo' },
    );
    expect(campos.filter((campo) => campo.fueModificado)).toEqual([
      { campo: 'País', valorAnterior: 'Colombia', valorNuevo: 'Perú', fueModificado: true },
    ]);
  });

  it('al crear no hay valores anteriores ni campos modificados', () => {
    const campos = compararDatos(undefined, { Nombre: 'Javeriana', Cupo: 3 });
    expect(campos).toEqual([
      { campo: 'Nombre', valorAnterior: '—', valorNuevo: 'Javeriana', fueModificado: false },
      { campo: 'Cupo', valorAnterior: '—', valorNuevo: '3', fueModificado: false },
    ]);
  });

  it('incluye campos que solo existen antes o después', () => {
    const campos = compararDatos({ Nombre: 'Ana' }, { Nombre: 'Ana', Contraseña: 'Se cambió' });
    expect(campos.map((campo) => campo.campo)).toEqual(['Nombre', 'Contraseña']);
    expect(campos[1].fueModificado).toBe(true);
  });
});

describe('claseBadgeAccion', () => {
  it('distingue acciones que suman, quitan o informan', () => {
    expect(claseBadgeAccion('CREÓ')).toContain('badge--activo');
    expect(claseBadgeAccion('ELIMINÓ')).toContain('badge--inactivo');
    expect(claseBadgeAccion('IMPORTÓ')).toContain('badge--info');
  });
});
