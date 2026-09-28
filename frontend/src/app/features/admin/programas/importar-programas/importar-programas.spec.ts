import { TestBed } from '@angular/core/testing';
import { ProgramaService } from '../../../../services/programa.service';
import { ImportarProgramas } from './importar-programas';

describe('ImportarProgramas', () => {
  it('should create', () => {
    TestBed.configureTestingModule({
      imports: [ImportarProgramas],
      providers: [{ provide: ProgramaService, useValue: {} }],
    });
    const fixture = TestBed.createComponent(ImportarProgramas);
    fixture.componentRef.setInput('universidades', []);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });
});
