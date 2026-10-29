import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ResidentesACargo } from '../../../../models/coordinador.model';
import { DashboardDatos } from '../../../../models/dashboard.model';
import { CoordinadorService } from '../../../../services/coordinador.service';
import { DashboardService } from '../../../../services/dashboard.service';
import { DashboardCoordinador } from './dashboard-coordinador';

const PANEL: DashboardDatos = {
  indicadores: [],
  situacionPorPrograma: [],
  cargaResidentes: [],
  mapaOcupacion: { meses: ['2026-10'], filas: [] },
  rotacionesPorServicio: [],
  distribucionUniversidad: [],
  tendenciaInicios: [{ etiqueta: 'oct', valor: 3 }],
  alertas: [],
  revisar: [],
  totalRegistros: 0,
};

const RESIDENTES: ResidentesACargo = {
  servicio: 'Urgencias',
  fecha: '2026-10-14',
  residentes: [
    {
      residenteId: 1,
      nombre: 'María José Osorio',
      documento: 'CC 1001',
      universidad: 'Universidad Javeriana',
      programa: 'Medicina interna',
      asignatura: 'Urgencias I',
      inicio: '2026-10-01',
      fin: '2026-10-31',
    },
  ],
  proximos: [
    {
      residenteId: 2,
      nombre: 'Julián Camilo Zapata',
      documento: 'CC 1002',
      universidad: 'Universidad de los Andes',
      programa: 'Convenio Nacional',
      asignatura: 'Urgencias I',
      inicio: '2026-11-01',
      fin: '2026-11-30',
    },
  ],
};

describe('DashboardCoordinador', () => {
  let component: DashboardCoordinador;
  let fixture: ComponentFixture<DashboardCoordinador>;
  let dashboardService: { consultarCoordinador: ReturnType<typeof vi.fn> };
  let coordinadorService: { listarResidentes: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    dashboardService = { consultarCoordinador: vi.fn().mockReturnValue(of(PANEL)) };
    coordinadorService = { listarResidentes: vi.fn().mockReturnValue(of(RESIDENTES)) };
    await TestBed.configureTestingModule({
      imports: [DashboardCoordinador],
      providers: [
        { provide: DashboardService, useValue: dashboardService },
        { provide: CoordinadorService, useValue: coordinadorService },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DashboardCoordinador);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra el servicio del coordinador y cuenta las rotaciones por asignatura', () => {
    const texto: string = fixture.nativeElement.textContent;
    expect(texto).toContain('Urgencias');
    // Las dos filas (en curso y próxima) son de «Urgencias I»: una sola barra con valor 2.
    expect(component['rotacionesPorAsignatura']()).toEqual([
      { etiqueta: 'Urgencias I', valor: 2, maximo: 2 },
    ]);
  });
});
