import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PanelDashboard } from './panel-dashboard';

describe('PanelDashboard', () => {
  let component: PanelDashboard;
  let fixture: ComponentFixture<PanelDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
      imports: [PanelDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(PanelDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
