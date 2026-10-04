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
      reconstruir: false,
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

  it('conserva las correcciones al confirmar las mismas columnas y las descarta al reconstruir', () => {
    const asistente = crearAsistente();
    const configuracion = prepararFilas(asistente);
    asistente.actualizarFilas([crearFilaDePrueba({ residente: 'Corregido' })]);
    asistente.aplicarColumnas({ configuracion, filas: [crearFilaDePrueba()], reconstruir: false });
    expect(asistente.filas()[0].residente).toBe('Corregido');
    asistente.aplicarColumnas({ configuracion, filas: [crearFilaDePrueba()], reconstruir: true });
    expect(asistente.filas()[0].residente).toBe('Ana Pérez');
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
