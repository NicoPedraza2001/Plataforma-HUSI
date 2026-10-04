import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { ServicioRequest } from '../../../../../models/servicio.model';
import { ServicioService } from '../../../../../services/servicio.service';
import {
  crearFilaDePrueba,
  crearPaginaDePrueba,
  esperarCargas,
  SERVICIO_ALTO_RIESGO_INACTIVO,
  SERVICIO_UCI,
} from '../datos-de-prueba';
import { emparejarServicios, leerCuposDelGrupo } from './emparejar-servicios';
import { PasoServicios } from './paso-servicios';

describe('emparejarServicios', () => {
  it('clasifica existentes, nuevos con sugerencias, inactivos y cupos distintos', () => {
    const filas = [
      crearFilaDePrueba({ fila: 2, servicio: 'uci', cupoMaximo: '', cupoSimultaneo: '' }),
      crearFilaDePrueba({ fila: 3, servicio: 'U.C.I', cupoMaximo: '8', cupoSimultaneo: '4' }),
      crearFilaDePrueba({ fila: 4, servicio: 'Alto Riesgo' }),
      crearFilaDePrueba({ fila: 5, servicio: 'UCI', cupoMaximo: '12', cupoSimultaneo: '4' }),
    ];
    const grupos = emparejarServicios(filas, new Map(), [
      SERVICIO_UCI,
      SERVICIO_ALTO_RIESGO_INACTIVO,
    ]);
    const estadoPorValor = Object.fromEntries(
      grupos.map((grupo) => [grupo.valorEnExcel, grupo.estado]),
    );
    expect(estadoPorValor['uci']).toBe('diferencias');
    expect(estadoPorValor['U.C.I']).toBe('nuevo');
    expect(estadoPorValor['Alto Riesgo']).toBe('inactivo');
    const grupoUci = grupos.find((grupo) => grupo.valorEnExcel === 'uci')!;
    expect(grupoUci.observaciones[0]).toContain('Cupo máximo en el Excel: 12; en el sistema: 8.');
  });

  it('acepta cupos vacíos en un servicio existente, como el backend', () => {
    const [grupo] = emparejarServicios(
      [crearFilaDePrueba({ cupoMaximo: '', cupoSimultaneo: '' })],
      new Map(),
      [SERVICIO_UCI],
    );
    expect(grupo.estado).toBe('encontrado');
  });

  it('solo toma cupos válidos para crear: enteros y simultáneo sin superar el máximo', () => {
    expect(
      leerCuposDelGrupo([crearFilaDePrueba({ cupoMaximo: '6', cupoSimultaneo: '3' })]),
    ).toEqual({ cupoMaximoInstalado: 6, cupoSimultaneo: 3 });
    expect(
      leerCuposDelGrupo([crearFilaDePrueba({ cupoMaximo: '2', cupoSimultaneo: '3' })]),
    ).toBeNull();
    expect(
      leerCuposDelGrupo([crearFilaDePrueba({ cupoMaximo: 'seis', cupoSimultaneo: '3' })]),
    ).toBeNull();
  });
});

describe('PasoServicios', () => {
  async function crearPaso(filas: FilaCargaInicial[]) {
    const servicioServiceFalso = {
      listar: () => of(crearPaginaDePrueba([SERVICIO_UCI])),
      crear: (datos: ServicioRequest) => of({ ...datos, id: 30, estado: 'Activo', activo: true }),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: ServicioService, useValue: servicioServiceFalso }],
    });
    const fixture = TestBed.createComponent(PasoServicios);
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

  it('usar datos del sistema reescribe los cupos con los registrados', async () => {
    const { paso, filasEmitidas } = await crearPaso([
      crearFilaDePrueba({ servicio: 'UCI', cupoMaximo: '12', cupoSimultaneo: '4' }),
    ]);
    paso.usarDatosDelSistema(paso.grupos()[0]);
    expect(filasEmitidas[0][0].cupoMaximo).toBe('8');
    expect(filasEmitidas[0][0].cupoSimultaneo).toBe('4');
  });

  it('crea en lote solo los nuevos con cupos válidos y sin parecidos', async () => {
    const { paso, filasEmitidas } = await crearPaso([
      crearFilaDePrueba({ fila: 2, servicio: 'Neonatos', cupoMaximo: '6', cupoSimultaneo: '2' }),
      crearFilaDePrueba({ fila: 3, servicio: 'Pediatría', cupoMaximo: '', cupoSimultaneo: '' }),
    ]);
    expect(paso.serviciosParaCrearEnLote()).toHaveLength(1);
    await paso.crearServiciosEnLote();
    expect(filasEmitidas[0][0].servicio).toBe('Neonatos');
    expect(paso.grupos().find((grupo: any) => grupo.valorEnExcel === 'Neonatos').estado).toBe(
      'encontrado',
    );
  });

  it('precarga el formulario con los cupos del Excel', async () => {
    const { paso } = await crearPaso([
      crearFilaDePrueba({ servicio: 'Neonatos', cupoMaximo: '6', cupoSimultaneo: '2' }),
    ]);
    paso.abrirCreacion(paso.grupos()[0]);
    expect(paso.datosNuevoServicio()).toEqual({
      nombre: 'Neonatos',
      descripcion: null,
      cupoMaximoInstalado: 6,
      cupoSimultaneo: 2,
    });
  });
});
