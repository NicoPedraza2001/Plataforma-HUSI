import { TestBed } from '@angular/core/testing';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { crearFilaDePrueba } from '../datos-de-prueba';
import { RevisarCarga } from './revisar-carga';

describe('RevisarCarga', () => {
  function crearRevision() {
    const fixture = TestBed.createComponent(RevisarCarga);
    fixture.componentRef.setInput('filas', [
      crearFilaDePrueba({ fila: 2 }),
      crearFilaDePrueba({ fila: 3 }),
    ]);
    fixture.detectChanges();
    const revision = fixture.componentInstance as any;
    let filasRecibidas: FilaCargaInicial[] = [];
    revision.cambiar.subscribe((filas: FilaCargaInicial[]) => (filasRecibidas = filas));
    return { revision, filasRecibidas: () => filasRecibidas };
  }

  it('cambia prioridad sin cambiar el número original de fila', () => {
    const { revision, filasRecibidas } = crearRevision();
    revision.moverFila({ fila: 3 }, -1);
    expect(filasRecibidas().map((fila) => fila.fila)).toEqual([3, 2]);
  });

  it('abre el editor de una fila por su número del Excel', () => {
    const { revision } = crearRevision();
    revision.editarFilaPorNumero(3);
    expect(revision.filaEnEdicion().fila).toBe(3);
    expect(revision.filasVisibles()).toHaveLength(1);
  });
});
