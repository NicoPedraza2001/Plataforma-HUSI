import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AsignaturaRequest } from '../../../../../models/asignatura.model';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { AsignaturaService } from '../../../../../services/asignatura.service';
import { ServicioService } from '../../../../../services/servicio.service';
import {
  ASIGNATURA_CUIDADO_INTENSIVO,
  crearFilaDePrueba,
  crearPaginaDePrueba,
  esperarCargas,
  SERVICIO_ALTO_RIESGO_INACTIVO,
  SERVICIO_UCI,
} from '../datos-de-prueba';
import { emparejarAsignaturas } from './emparejar-asignaturas';
import { PasoAsignaturas } from './paso-asignaturas';

describe('emparejarAsignaturas', () => {
  it('busca la asignatura dentro de su servicio y sugiere las parecidas', () => {
    const filas = [
      crearFilaDePrueba({ fila: 2 }),
      crearFilaDePrueba({ fila: 3, asignatura: 'Cuidado Intensivo I' }),
      crearFilaDePrueba({ fila: 4, servicio: 'Servicio sin registrar' }),
    ];
    const grupos = emparejarAsignaturas(
      filas,
      new Map(),
      [SERVICIO_UCI],
      [ASIGNATURA_CUIDADO_INTENSIVO],
    );
    expect(grupos.map((grupo) => grupo.estado)).toEqual(['encontrado', 'nuevo', 'incompleto']);
    expect(grupos[1].sugerencias[0].id).toBe(ASIGNATURA_CUIDADO_INTENSIVO.id);
    expect(grupos[2].sePuedeCrear).toBe(false);
  });
});

describe('PasoAsignaturas', () => {
  async function crearPaso(filas: FilaCargaInicial[]) {
    const asignaturaServiceFalso = {
      listar: () => of(crearPaginaDePrueba([ASIGNATURA_CUIDADO_INTENSIVO])),
      crear: (datos: AsignaturaRequest) => {
        const servicio = [SERVICIO_UCI, SERVICIO_ALTO_RIESGO_INACTIVO].find(
          (servicioRegistrado) => servicioRegistrado.id === datos.servicioId,
        )!;
        return of({
          id: 50,
          nombre: datos.nombre,
          servicioId: servicio.id,
          servicio: servicio.nombre,
          estado: 'Activo',
          activo: true,
        });
      },
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: AsignaturaService, useValue: asignaturaServiceFalso },
        {
          provide: ServicioService,
          useValue: {
            listar: () => of(crearPaginaDePrueba([SERVICIO_UCI, SERVICIO_ALTO_RIESGO_INACTIVO])),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(PasoAsignaturas);
    fixture.componentRef.setInput('filas', filas);
    fixture.componentRef.setInput('filasOriginales', filas);
    fixture.detectChanges();
    await esperarCargas();
    fixture.detectChanges();
    const paso = fixture.componentInstance as any;
    const filasEmitidas: FilaCargaInicial[][] = [];
    paso.filasCambiadas.subscribe((filasNuevas: FilaCargaInicial[]) =>
      filasEmitidas.push(filasNuevas),
    );
    return { paso, filasEmitidas };
  }

  it('should create', async () => {
    expect((await crearPaso([crearFilaDePrueba()])).paso).toBeTruthy();
  });

  it('crea la asignatura en el servicio de sus filas', async () => {
    const { paso, filasEmitidas } = await crearPaso([
      crearFilaDePrueba({ asignatura: 'Ventilación mecánica' }),
    ]);
    paso.abrirCreacion(paso.grupos()[0]);
    expect(paso.datosNuevaAsignatura()).toEqual({
      nombre: 'Ventilación mecánica',
      servicioId: SERVICIO_UCI.id,
    });
    await paso.guardarAsignaturaNueva();
    expect(filasEmitidas[0][0].asignatura).toBe('Ventilación mecánica');
    expect(filasEmitidas[0][0].servicio).toBe('UCI');
  });

  it('si al crearla se elige otro servicio, las filas toman ese servicio y sus cupos', async () => {
    const { paso, filasEmitidas } = await crearPaso([
      crearFilaDePrueba({ asignatura: 'Obstetricia', cupoMaximo: '8', cupoSimultaneo: '4' }),
    ]);
    paso.abrirCreacion(paso.grupos()[0]);
    paso.datosNuevaAsignatura().servicioId = SERVICIO_ALTO_RIESGO_INACTIVO.id;
    await paso.guardarAsignaturaNueva();
    expect(filasEmitidas[0][0].servicio).toBe('Alto Riesgo');
    expect(filasEmitidas[0][0].cupoMaximo).toBe('10');
  });

  it('solo ofrece servicios activos en el formulario', async () => {
    const { paso } = await crearPaso([crearFilaDePrueba()]);
    expect(paso.opcionesDeServicio().map((opcion: { nombre: string }) => opcion.nombre)).toEqual([
      'UCI',
    ]);
  });
});
