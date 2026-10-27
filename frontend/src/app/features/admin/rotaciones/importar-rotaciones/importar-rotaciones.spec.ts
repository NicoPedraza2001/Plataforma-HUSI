import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CAMPOS_CARGA } from './campos-carga';
import { crearFilaDePrueba } from './datos-de-prueba';
import { ImportarRotaciones } from './importar-rotaciones';

const LIBRO_DE_PRUEBA = {
  nombreArchivo: 'datos.xlsx',
  hojas: [{ nombre: 'Datos', filas: [CAMPOS_CARGA.map((campo) => campo.nombre)] }],
};

describe('ImportarRotaciones (asistente)', () => {
  function crearAsistente() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(ImportarRotaciones);
    fixture.detectChanges();
    return fixture.componentInstance as any;
  }

  function prepararFilas(asistente: any) {
    asistente.registrarArchivo(LIBRO_DE_PRUEBA);
    const configuracion = asistente.configuracionColumnas();
    asistente.aplicarColumnas({
      configuracion,
      filas: [crearFilaDePrueba()],
    });
    return configuracion;
  }

  it('should create', () => {
    expect(crearAsistente()).toBeTruthy();
  });

  it('solo habilita cada paso de emparejamiento cuando el anterior no tiene pendientes', () => {
    const asistente = crearAsistente();
    expect(asistente.estaDisponible('columnas')).toBe(false);
    prepararFilas(asistente);
    expect(asistente.pasoActual()).toBe('universidades');
    expect(asistente.estaDisponible('programas')).toBe(false);
    asistente.registrarPendientes('universidades', 2);
    expect(asistente.estaDisponible('programas')).toBe(false);
    asistente.registrarPendientes('universidades', 0);
    expect(asistente.estaDisponible('programas')).toBe(true);
    expect(asistente.estaDisponible('residentes')).toBe(false);
    asistente.registrarPendientes('programas', 0);
    expect(asistente.estaDisponible('servicios')).toBe(true);
    expect(asistente.estaDisponible('residentes')).toBe(false);
    asistente.registrarPendientes('servicios', 0);
    asistente.registrarPendientes('asignaturas', 0);
    asistente.registrarPendientes('residentes', 0);
    expect(asistente.estaDisponible('rotaciones')).toBe(true);
    expect(asistente.estaDisponible('confirmacion')).toBe(false);
  });

  it('conserva las correcciones al confirmar las mismas columnas y las descarta si cambian', () => {
    const asistente = crearAsistente();
    const configuracion = prepararFilas(asistente);
    asistente.actualizarFilas([crearFilaDePrueba({ residente: 'Corregido' })]);
    asistente.aplicarColumnas({ configuracion, filas: [crearFilaDePrueba()] });
    expect(asistente.filas()[0].residente).toBe('Corregido');
    asistente.aplicarColumnas({
      configuracion: { ...configuracion, filaEncabezados: 2 },
      filas: [crearFilaDePrueba()],
    });
    expect(asistente.filas()[0].residente).toBe('Ana Pérez');
  });

  it('guarda las filas excluidas tal como estaban y las restaura en su paso', () => {
    const asistente = crearAsistente();
    asistente.registrarArchivo(LIBRO_DE_PRUEBA);
    asistente.aplicarColumnas({
      configuracion: asistente.configuracionColumnas(),
      filas: [crearFilaDePrueba({ fila: 2 }), crearFilaDePrueba({ fila: 3 })],
    });
    asistente.registrarPendientes('universidades', 0);
    asistente.irAPaso('programas');
    asistente.actualizarFilas([
      crearFilaDePrueba({ fila: 2 }),
      crearFilaDePrueba({ fila: 3, programa: 'Corregido' }),
    ]);
    asistente.actualizarFilas([crearFilaDePrueba({ fila: 2 })]);
    expect(asistente.filasExcluidas().map((fila: any) => fila.programa)).toEqual(['Corregido']);
    expect(asistente.excluidas()[0].paso).toBe('programas');
    expect(asistente.mostrarExcluidas()).toBe(true);

    asistente.registrarPendientes('programas', 0);
    asistente.registrarPendientes('servicios', 0);
    asistente.irAPaso('servicios');
    asistente.registrarValidacion({ validos: 1, filas: [] });
    asistente.restaurarFilas([3]);

    expect(asistente.filas().map((fila: any) => fila.fila)).toEqual([2, 3]);
    expect(asistente.filas()[1].programa).toBe('Corregido');
    expect(asistente.excluidas()).toEqual([]);
    expect(asistente.pasoActual()).toBe('programas');
    expect(asistente.pendientesPorPaso()).toEqual({ universidades: 0 });
    expect(asistente.estaDisponible('servicios')).toBe(false);
    expect(asistente.resultadoValidacion()).toBeNull();
  });

  it('cualquier corrección invalida la validación anterior', () => {
    const asistente = crearAsistente();
    prepararFilas(asistente);
    asistente.registrarValidacion({ validos: 1, filas: [] });
    asistente.actualizarFilas([crearFilaDePrueba({ servicio: 'Otro' })]);
    expect(asistente.resultadoValidacion()).toBeNull();
  });

  it('si la confirmación falla vuelve a Rotaciones y exige validar de nuevo', () => {
    const asistente = crearAsistente();
    prepararFilas(asistente);
    asistente.registrarValidacion({ validos: 1, filas: [] });
    asistente.registrarFalloDeConfirmacion('Cupos cambiaron');
    expect(asistente.pasoActual()).toBe('rotaciones');
    expect(asistente.resultadoValidacion()).toBeNull();
    expect(asistente.error()).toBe('Cupos cambiaron');
  });

  it('al terminar la carga solo deja ver el resultado', () => {
    const asistente = crearAsistente();
    prepararFilas(asistente);
    asistente.registrarCargaFinal({ validos: 1, filas: [] });
    expect(asistente.estaDisponible('archivo')).toBe(false);
    expect(asistente.estaDisponible('confirmacion')).toBe(true);
  });
});
