import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListadoUsuarios } from './listado-usuarios';

describe('ListadoUsuarios', () => {
  let component: ListadoUsuarios;
  let fixture: ComponentFixture<ListadoUsuarios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
      imports: [ListadoUsuarios],
    }).compileComponents();

    fixture = TestBed.createComponent(ListadoUsuarios);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
