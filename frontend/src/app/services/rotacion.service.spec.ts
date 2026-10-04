import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RotacionService } from './rotacion.service';
describe('RotacionService lista de espera', () => {
  let api: RotacionService;
  let http: HttpTestingController;
  const destino = { asignaturaId: 4, inicio: '2091-03-01', fin: '2091-03-31' };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(RotacionService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta la espera del servicio para la rotación de destino', () => {
    api.espera(destino).subscribe();
    const req = http.expectOne('/api/rotaciones/gestion/4/2091-03-01/2091-03-31/espera');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
  it('traslada los residentes elegidos con su rotación de origen', () => {
    const origen = { residenteId: 9, asignaturaId: 2, inicio: '2091-01-01', fin: '2091-01-31' };
    api.trasladarEspera(destino, [origen]).subscribe();
    const req = http.expectOne('/api/rotaciones/gestion/4/2091-03-01/2091-03-31/espera');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ residentes: [origen] });
    req.flush(null);
  });
  it('elimina la relación de un residente con la rotación', () => {
    api.eliminarResidente(destino, 9).subscribe();
    const req = http.expectOne('/api/rotaciones/gestion/4/2091-03-01/2091-03-31/residentes/9');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
