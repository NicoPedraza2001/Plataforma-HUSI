import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ResultadoCargaInicial } from '../../../../../models/carga-inicial.model';
import { crearFilaDePrueba } from '../datos-de-prueba';
import { IdPasoImportacion } from '../pasos-importacion';
import { PasoRotaciones } from './paso-rotaciones';

describe('PasoRotaciones', () => {
  function crearPaso() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(PasoRotaciones);
    fixture.componentRef.setInput('filas', [crearFilaDePrueba({ documento: '001' })]);
    fixture.detectChanges();
    return {
      paso: fixture.componentInstance as any,
      http: TestBed.inject(HttpTestingController),
    };
  }

  it('should create', () => {
    expect(crearPaso().paso).toBeTruthy();
  });

  it('valida las filas sin guardar y emite el resultado', async () => {
    const { paso, http } = crearPaso();
    let resultadoEmitido: ResultadoCargaInicial | null = null;
    paso.validacionCompletada.subscribe(
      (resultado: ResultadoCargaInicial) => (resultadoEmitido = resultado),
    );
    const validacion = paso.validarConLaBaseDeDatos();
    const solicitud = http.expectOne('/api/rotaciones/carga-inicial/validar');
    expect(solicitud.request.body.filas[0].documento).toBe('001');
    // Los faltantes se crean en sus pasos; la validación final no crea nada por su cuenta.
    expect(solicitud.request.body.crearFaltantes).toBe(false);
    solicitud.flush({ validos: 1, filas: [] });
    await validacion;
    expect(resultadoEmitido!.validos).toBe(1);
    http.verify();
  });

  it('envía al paso correspondiente los errores que no son de rotación', () => {
    const { paso } = crearPaso();
    let pasoSolicitado: IdPasoImportacion | null = null;
    paso.irAPaso.subscribe((idPaso: IdPasoImportacion) => (pasoSolicitado = idPaso));
    paso.corregirFila({ numeroFila: 2, paso: 'programas' });
    expect(pasoSolicitado).toBe('programas');
  });
});
