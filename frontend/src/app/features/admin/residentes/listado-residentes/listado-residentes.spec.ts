import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListadoResidentes } from './listado-residentes';

describe('ListadoResidentes', () => {
  let component: ListadoResidentes;
  let fixture: ComponentFixture<ListadoResidentes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
      imports: [ListadoResidentes],
    }).compileComponents();

    fixture = TestBed.createComponent(ListadoResidentes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
