import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { UniversidadService } from '../../../../../services/universidad.service';
import {
  crearFilaDePrueba,
  crearPaginaDePrueba,
  esperarCargas,
  UNIVERSIDAD_JAVERIANA,
  UNIVERSIDAD_ROSARIO_INACTIVA,
} from '../datos-de-prueba';
import { emparejarUniversidades } from './emparejar-universidades';
import { PasoUniversidades } from './paso-universidades';

describe('emparejarUniversidades', () => {
  it('clasifica existentes, nuevas con sugerencias, inactivas y diferencias de país', () => {
    const filas = [
      crearFilaDePrueba({ fila: 2 }),
      crearFilaDePrueba({ fila: 3, universidad: 'Univ. Javeriana' }),
      crearFilaDePrueba({ fila: 4, universidad: 'universidad del rosario' }),
      crearFilaDePrueba({ fila: 5, universidad: 'PONTIFICIA UNIVERSIDAD JAVERIANA', pais: 'Perú' }),
    ];
    const grupos = emparejarUniversidades(filas, new Map(), [
      UNIVERSIDAD_JAVERIANA,
      UNIVERSIDAD_ROSARIO_INACTIVA,
    ]);
    const estadoPorValor = Object.fromEntries(
      grupos.map((grupo) => [grupo.valorEnExcel, grupo.estado]),
    );
    expect(grupos).toHaveLength(3);
    expect(estadoPorValor['Pontificia Universidad Javeriana']).toBe('diferencias');
    expect(estadoPorValor['Univ. Javeriana']).toBe('nuevo');
    expect(estadoPorValor['universidad del rosario']).toBe('inactivo');
    const nueva = grupos.find((grupo) => grupo.valorEnExcel === 'Univ. Javeriana')!;
    expect(nueva.sugerencias[0].id).toBe(UNIVERSIDAD_JAVERIANA.id);
  });
});

describe('PasoUniversidades', () => {
  async function crearPaso(filas: FilaCargaInicial[]) {
    const universidadServiceFalso = {
      buscar: () => of(crearPaginaDePrueba([UNIVERSIDAD_JAVERIANA])),
      crear: (datos: { nombre: string; pais: string }) =>
        of({ id: 9, nombre: datos.nombre, pais: datos.pais, estado: 'Activo' }),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: UniversidadService, useValue: universidadServiceFalso }],
    });
    const fixture = TestBed.createComponent(PasoUniversidades);
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
    return { fixture, paso, filasEmitidas };
  }

  it('should create', async () => {
    expect((await crearPaso([crearFilaDePrueba()])).paso).toBeTruthy();
  });

  it('al asociar una existente reescribe nombre y país de las filas del grupo', async () => {
    const { paso, filasEmitidas } = await crearPaso([
      crearFilaDePrueba({ fila: 3, universidad: 'Javeriana', pais: '' }),
    ]);
    const grupoNuevo = paso.grupos()[0];
    expect(grupoNuevo.estado).toBe('nuevo');
    paso.asociarUniversidadExistente({ grupo: grupoNuevo, registroId: UNIVERSIDAD_JAVERIANA.id });
    expect(filasEmitidas[0][0].universidad).toBe(UNIVERSIDAD_JAVERIANA.nombre);
    expect(filasEmitidas[0][0].pais).toBe('Colombia');
  });

  it('crea la universidad desde el formulario y la asigna a sus filas', async () => {
    const { paso, filasEmitidas } = await crearPaso([
      crearFilaDePrueba({ fila: 3, universidad: 'Universidad Nueva', pais: 'Chile' }),
    ]);
    paso.abrirCreacion(paso.grupos()[0]);
    expect(paso.paisNuevaUniversidad()).toBe('Chile');
    await paso.guardarUniversidadNueva();
    expect(filasEmitidas[0][0].universidad).toBe('Universidad Nueva');
    expect(paso.grupoEnCreacion()).toBeNull();
  });

  it('bloquea el avance mientras haya universidades por resolver', async () => {
    const { fixture } = await crearPaso([crearFilaDePrueba({ universidad: 'Otra' })]);
    fixture.detectChanges();
    const botonContinuar = [...fixture.nativeElement.querySelectorAll('footer button')].find(
      (boton: HTMLButtonElement) => boton.textContent?.includes('Continuar'),
    ) as HTMLButtonElement;
    expect(botonContinuar.disabled).toBe(true);
  });
});
