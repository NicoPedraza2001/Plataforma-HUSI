import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UniversidadService } from './universidad.service';

describe('UniversidadService', () => {
  it('consulta el catálogo en UniversidadController', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpTestingController);

    TestBed.inject(UniversidadService).listar().subscribe();

    http.expectOne({ method: 'GET', url: '/api/universidades' });
    http.verify();
  });
});
