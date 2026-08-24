import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListadoProgramas } from './listado-programas';

describe('ListadoProgramas', () => {
  let component: ListadoProgramas;
  let fixture: ComponentFixture<ListadoProgramas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListadoProgramas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListadoProgramas);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
