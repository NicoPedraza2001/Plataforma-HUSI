import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GraficoApilado } from './grafico-apilado';

describe('GraficoApilado', () => {
  let component: GraficoApilado;
  let fixture: ComponentFixture<GraficoApilado>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraficoApilado],
    }).compileComponents();

    fixture = TestBed.createComponent(GraficoApilado);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('datos', []);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
