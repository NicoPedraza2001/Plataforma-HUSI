// Prueba optativa de validación contra localhost:8080; nunca guarda datos.
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CargaInicialService } from '../../../../services/carga-inicial.service';
import { crearFilaDePrueba } from './datos-de-prueba';

describe('Carga inicial contra backend local', () => {
  it('rechaza los faltantes sin crear registros', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withFetch(),
          withInterceptors([
            (solicitud, siguiente) =>
              siguiente(solicitud.clone({ url: 'http://localhost:8080' + solicitud.url })),
          ]),
        ),
      ],
    });
    const cargaInicialService = TestBed.inject(CargaInicialService);
    const filaVacia = crearFilaDePrueba({
      universidad: '',
      pais: '',
      programa: '',
      residente: '',
      tipoDocumento: '',
      documento: '',
      servicio: '',
      asignatura: '',
      inicio: '',
      fin: '',
    });
    const resultado = await firstValueFrom(cargaInicialService.validar([filaVacia], true));
    expect(resultado.errores).toBe(1);
    expect(resultado.guardado).toBe(false);
  });
});
