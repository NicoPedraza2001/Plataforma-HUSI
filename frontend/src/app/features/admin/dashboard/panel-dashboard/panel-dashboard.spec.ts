import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { DashboardDatos } from '../../../../models/dashboard.model';
import { AsignaturaService } from '../../../../services/asignatura.service';
import { CatalogoService } from '../../../../services/catalogo.service';
import { DashboardService } from '../../../../services/dashboard.service';
import { RotacionService } from '../../../../services/rotacion.service';
import { PanelDashboard } from './panel-dashboard';

const DATOS: DashboardDatos = {
  indicadores: [
    {
      id: 'cruces',
      etiqueta: 'Con cruce de fechas',
      valor: '1',
      detalle: '',
      tono: 'rojo',
    },
  ],
  situacionPorPrograma: [{ etiqueta: 'Especialización en Geriatría', conRotacion: 1, enEspera: 1 }],
  cargaResidentes: [{ etiqueta: '0 rotaciones', valor: 1 }],
  mapaOcupacion: { meses: [], filas: [] },
  rotacionesPorServicio: [],
  distribucionUniversidad: [{ etiqueta: 'Javeriana', porcentaje: 100 }],
  tendenciaInicios: [],
  alertas: [
    {
      id: 'cruce',
      nivel: 'critico',
      etiqueta: 'Residentes con cruce de fechas',
      detalle: '',
      cantidad: 1,
    },
  ],
  revisar: [
    {
      residenteId: 1,
      residente: 'Ana',
      programa: 'Geriatría',
      motivo: 'cruce',
      detalle: ['cruce 1', 'cruce 2', 'cruce 3'].join('\n'),
    },
    {
      residenteId: 2,
      residente: 'Luis',
      programa: 'Geriatría',
      motivo: 'espera',
      detalle: 'y',
    },
  ],
  totalRegistros: 3,
};

describe('PanelDashboard', () => {
  let component: PanelDashboard;
  let fixture: ComponentFixture<PanelDashboard>;
  let dashboard: { consultar: ReturnType<typeof vi.fn>; relaciones: ReturnType<typeof vi.fn> };
  let rotaciones: { listar: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    // jsdom no implementa el desplazamiento que hacen los avisos al pulsarse.
    Element.prototype.scrollIntoView = vi.fn();
    dashboard = {
      consultar: vi.fn(() => of(DATOS)),
      // Geriatría solo rota en Alto Riesgo; Medicina Interna, en Cirugía.
      relaciones: vi.fn(() =>
        of([
          { programa: 'Geriatría', servicio: 'Alto Riesgo', asignaturaId: 7 },
          { programa: 'Medicina Interna', servicio: 'Cirugía', asignaturaId: 8 },
        ]),
      ),
    };
    rotaciones = {
      listar: vi.fn(() => of({ contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1 })),
    };
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: dashboard },
        { provide: RotacionService, useValue: rotaciones },
        {
          provide: CatalogoService,
          useValue: {
            listar: () =>
              of({
                periodos: ['2001-2', '2001-1'],
                universidades: [],
                programas: ['Geriatría', 'Medicina Interna'],
                servicios: ['Alto Riesgo', 'Cirugía'],
              }),
          },
        },
        {
          provide: AsignaturaService,
          useValue: {
            catalogo: () =>
              of([
                { id: 7, nombre: 'Cadera', servicio: 'Alto Riesgo' },
                { id: 8, nombre: 'Cirugía oculoplástica', servicio: 'Cirugía' },
              ]),
          },
        },
      ],
      imports: [PanelDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(PanelDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('abre en el periodo más reciente con datos cuando el semestre en curso no tiene', () => {
    expect(dashboard.consultar).toHaveBeenCalledTimes(1);
    expect(dashboard.consultar).toHaveBeenCalledWith({ periodo: '2001-2' });
    expect(filtro('Periodo').value).toBe('2001-2');
  });

  it('las próximas rotaciones no dependen del periodo', () => {
    expect(rotaciones.listar).toHaveBeenCalledWith({ pagina: 1, tamano: 10, proximas: true });
  });

  it('oculta el reparto por universidad cuando solo hay una', () => {
    const titulos = [...fixture.nativeElement.querySelectorAll('.panel__titulo')].map(
      (titulo: Element) => titulo.textContent?.trim(),
    );
    expect(titulos).not.toContain('Distribución por universidad');
  });

  it('el aviso de cruces filtra la tabla de residentes a revisar', async () => {
    const filas = () =>
      [...fixture.nativeElement.querySelectorAll('#revisar tbody tr')].map((fila: Element) =>
        fila.querySelector('td')?.textContent?.trim(),
      );
    expect(filas()).toEqual(['Ana', 'Luis']);

    fixture.nativeElement.querySelector('.alerta--boton').click();
    await fixture.whenStable();

    expect(filas()).toEqual(['Ana']);
  });

  const filtro = (etiqueta: string) =>
    [...fixture.nativeElement.querySelectorAll('.filtro')]
      .find((f: Element) => f.querySelector('.filtro__label')?.textContent?.trim() === etiqueta)
      .querySelector('select') as HTMLSelectElement;
  const elegir = async (etiqueta: string, valor: string) => {
    const select = filtro(etiqueta);
    select.value = valor;
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    return select;
  };
  const opciones = (etiqueta: string) =>
    [...filtro(etiqueta).options].map((opcion) => opcion.textContent?.trim());

  it('cada filtro actualiza el panel al instante, sin botón Actualizar', async () => {
    const consultas = dashboard.consultar.mock.calls.length;
    await elegir('Periodo', '2001-1');
    expect(dashboard.consultar.mock.calls.length).toBe(consultas + 1);
    expect(dashboard.consultar).toHaveBeenLastCalledWith({ periodo: '2001-1' });
    // Sin opción «Todos»: cada periodo cuenta solo a sus residentes.
    expect(opciones('Periodo')).toEqual(['2001-2', '2001-1']);
    const botones = [...fixture.nativeElement.querySelectorAll('.filtros button')].map(
      (boton: Element) => boton.textContent?.trim(),
    );
    expect(botones).toEqual(['Limpiar filtros']);
  });

  it('encadena programa y servicio en ambos sentidos', async () => {
    await elegir('Programa', 'Geriatría');
    expect(opciones('Servicio')).toEqual(['Todos', 'Alto Riesgo']);
    expect(opciones('Asignatura')).toEqual(['Todas', 'Cadera · Alto Riesgo']);

    await elegir('Programa', 'Todos');
    await elegir('Servicio', 'Cirugía');
    expect(opciones('Programa')).toEqual(['Todos', 'Medicina Interna']);
  });

  it('limpiar filtros vuelve al semestre actual sin más filtros', async () => {
    await elegir('Programa', 'Geriatría');
    const limpiar = fixture.nativeElement.querySelector('.filtros .limpiar') as HTMLButtonElement;
    expect(limpiar.disabled).toBe(false);

    limpiar.click();
    await fixture.whenStable();

    expect(dashboard.consultar).toHaveBeenLastCalledWith({ periodo: '2001-2' });
    expect(filtro('Programa').value).toBe('Todos');
    expect(limpiar.disabled).toBe(true);
  });

  it('acota las asignaturas al servicio y envía programa y asignatura a ambas consultas', async () => {
    const asignatura = await elegir('Asignatura', '7');
    expect(opciones('Asignatura')).toEqual([
      'Todas',
      'Cadera · Alto Riesgo',
      'Cirugía oculoplástica · Cirugía',
    ]);

    // Al elegir otro servicio, la asignatura de Alto Riesgo deja de aplicar.
    await elegir('Servicio', 'Cirugía');
    expect(opciones('Asignatura')).toEqual(['Todas', 'Cirugía oculoplástica']);
    expect(asignatura.value).toBe('Todas');

    await elegir('Asignatura', '8');
    await elegir('Programa', 'Medicina Interna');

    expect(dashboard.consultar).toHaveBeenLastCalledWith(
      expect.objectContaining({
        programa: 'Medicina Interna',
        servicio: 'Cirugía',
        asignaturaId: '8',
      }),
    );
    expect(rotaciones.listar).toHaveBeenLastCalledWith(
      expect.objectContaining({ programa: 'Medicina Interna', asignaturaId: '8', proximas: true }),
    );
    expect(filtro('Asignatura').value).toBe('8');
  });
});
