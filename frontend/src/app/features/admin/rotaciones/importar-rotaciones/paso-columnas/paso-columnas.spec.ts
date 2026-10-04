import { TestBed } from '@angular/core/testing';
import { CAMPOS_CARGA } from '../campos-carga';
import { ColumnasConfirmadas, PasoColumnas } from './paso-columnas';

const libroDePrueba = {
  nombreArchivo: 'datos.xlsx',
  hojas: [
    {
      nombre: 'Datos',
      filas: [
        CAMPOS_CARGA.map((campo) => campo.nombre),
        ['Javeriana', 'Colombia', 'Medicina', 'Ana Pérez', 'CC', '001'],
      ],
    },
  ],
};

describe('PasoColumnas', () => {
  function crearPaso() {
    const fixture = TestBed.createComponent(PasoColumnas);
    fixture.componentRef.setInput('libroExcel', libroDePrueba);
    fixture.detectChanges();
    return { fixture, paso: fixture.componentInstance as any };
  }

  it('should create', () => {
    expect(crearPaso().paso).toBeTruthy();
  });

  it('detecta las columnas y emite las filas preparadas', () => {
    const { paso } = crearPaso();
    let confirmadas: ColumnasConfirmadas | null = null;
    paso.columnasConfirmadas.subscribe((evento: ColumnasConfirmadas) => (confirmadas = evento));
    paso.confirmarColumnas();
    expect(confirmadas!.filas).toHaveLength(1);
    expect(confirmadas!.filas[0].documento).toBe('001');
    expect(confirmadas!.configuracion.columnaPorCampo[0]).toBe(0);
  });

  it('rechaza dos campos que usan la misma columna', () => {
    const { paso } = crearPaso();
    paso.columnaPorCampo[1] = 0;
    paso.confirmarColumnas();
    expect(paso.error()).toContain('solo puede alimentar un campo');
  });

  it('recupera la configuración guardada al volver al paso', () => {
    const fixture = TestBed.createComponent(PasoColumnas);
    fixture.componentRef.setInput('libroExcel', libroDePrueba);
    fixture.componentRef.setInput('configuracionGuardada', {
      indiceHoja: 0,
      filaEncabezados: 1,
      columnaPorCampo: CAMPOS_CARGA.map(() => -1),
      valorComunPorCampo: CAMPOS_CARGA.map(() => 'común'),
    });
    fixture.detectChanges();
    const paso = fixture.componentInstance as any;
    expect(paso.valorComunPorCampo[0]).toBe('común');
  });
});
