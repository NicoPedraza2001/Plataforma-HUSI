import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PerfilAcademicoService } from './perfil-academico.service';

describe('PerfilAcademicoService', () => {
  let service: PerfilAcademicoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PerfilAcademicoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('usa la ruta y el verbo de PerfilAcademicoController', () => {
    service.listar().subscribe();
    http.expectOne({ method: 'GET', url: '/api/perfiles-academicos' });
  });
});
