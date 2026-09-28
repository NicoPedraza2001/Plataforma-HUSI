// Prueba optativa de lectura y validación contra localhost:8080; nunca guarda datos.
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ImportarRotaciones } from './importar-rotaciones';
describe('Carga inicial contra backend local', () => {
  it('rechaza los faltantes sin crear registros', async () => {
    TestBed.configureTestingModule({
      imports: [ImportarRotaciones],
      providers: [
        provideHttpClient(
          withFetch(),
          withInterceptors([(r, next) => next(r.clone({ url: 'http://localhost:8080' + r.url }))]),
        ),
      ],
    });
    const c = TestBed.createComponent(ImportarRotaciones).componentInstance as any;
    c.filas.set([
      {
        fila: 2,
        universidad: '',
        pais: '',
        programa: '',
        residente: '',
        tipoDocumento: '',
        documento: '',
        perfilAcademico: '',
        servicio: '',
        cupoMaximo: '',
        cupoSimultaneo: '',
        asignatura: '',
        inicio: '',
        fin: '',
      },
    ]);
    await c.validar();
    expect(c.error()).toBe('');
    expect(c.resultado().errores).toBe(1);
    expect(c.terminado()).toBe(false);
  });
});
