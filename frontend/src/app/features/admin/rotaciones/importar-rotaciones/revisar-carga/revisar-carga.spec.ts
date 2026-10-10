import { TestBed } from '@angular/core/testing';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { crearFilaDePrueba } from '../datos-de-prueba';
import { RevisarCarga } from './revisar-carga';

describe('RevisarCarga', () => {
  function crearRevision() {
    const fixture = TestBed.createComponent(RevisarCarga);
    fixture.componentRef.setInput('filas', [
      crearFilaDePrueba({ fila: 2 }),
      crearFilaDePrueba({ fila: 3, inicio: '31/11/2026', fin: '30/12/2026' }),
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

  it('una fecha inexistente deja el selector vacío y muestra lo que decía el Excel', () => {
    const { revision } = crearRevision();
    revision.editarFilaPorNumero(3);
    expect(revision.filaEnEdicion().inicio).toBe('');
    expect(revision.filaEnEdicion().fin).toBe('2026-12-30');
    expect(revision.fechaOriginalInvalida('inicio')).toBe('31/11/2026');
  });

  it('no guarda la corrección hasta que las fechas sean válidas', () => {
    const { revision, filasRecibidas } = crearRevision();
    revision.editarFilaPorNumero(3);
    revision.guardarEdicion();
    expect(revision.errorEdicion()).toContain('Indica');
    expect(filasRecibidas()).toEqual([]);

    revision.filaEnEdicion().inicio = '2026-11-30';
    revision.guardarEdicion();
    expect(filasRecibidas().find((fila) => fila.fila === 3)?.inicio).toBe('2026-11-30');
  });
});
