import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ResultadoCargaInicial } from '../../../../../models/carga-inicial.model';
import { crearFilaDePrueba } from '../datos-de-prueba';
import { PasoConfirmacion } from './paso-confirmacion';

const RESULTADO_VALIDADO: ResultadoCargaInicial = {
  validacion: 'huella',
  guardado: false,
  total: 1,
  validos: 1,
  espera: 0,
  errores: 0,
  omitidos: 0,
  nuevos: { rotacion_residente: 1 },
  filas: [{ fila: 2, estado: 'VALIDO', mensaje: 'Correcto' }],
};

describe('PasoConfirmacion', () => {
  function crearPaso() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(PasoConfirmacion);
    fixture.componentRef.setInput('filas', [crearFilaDePrueba()]);
    fixture.componentRef.setInput('resultadoValidacion', RESULTADO_VALIDADO);
    fixture.detectChanges();
    return { paso: fixture.componentInstance as any, http: TestBed.inject(HttpTestingController) };
  }

  it('should create', () => {
    expect(crearPaso().paso.registrosNuevos()[0].nombre).toBe('Asignaciones de residentes');
  });

  it('confirma con la huella de la validación', async () => {
    const { paso, http } = crearPaso();
    let resultadoGuardado: ResultadoCargaInicial | null = null;
    paso.cargaConfirmada.subscribe(
      (resultado: ResultadoCargaInicial) => (resultadoGuardado = resultado),
    );
    const confirmacion = paso.confirmarCarga();
    const solicitud = http.expectOne('/api/rotaciones/carga-inicial/confirmar');
    expect(solicitud.request.body.validacion).toBe('huella');
    solicitud.flush({ ...RESULTADO_VALIDADO, guardado: true });
    await confirmacion;
    expect(resultadoGuardado!.guardado).toBe(true);
    http.verify();
  });

  it('avisa del fallo para que el asistente obligue a revalidar', async () => {
    const { paso, http } = crearPaso();
    let mensajeDeFallo = '';
    paso.confirmacionFallida.subscribe((mensaje: string) => (mensajeDeFallo = mensaje));
    const confirmacion = paso.confirmarCarga();
    http
      .expectOne('/api/rotaciones/carga-inicial/confirmar')
      .flush({ mensaje: 'Cupos cambiaron' }, { status: 409, statusText: 'Conflict' });
    await confirmacion;
    expect(mensajeDeFallo).toContain('Cupos cambiaron');
    http.verify();
  });
});
