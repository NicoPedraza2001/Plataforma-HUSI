import { TestBed } from '@angular/core/testing';
import { RevisarCarga } from './revisar-carga';
describe('RevisarCarga', () => {
  it('cambia prioridad sin cambiar el número original de fila', () => {
    const f = TestBed.createComponent(RevisarCarga),
      c = f.componentInstance as any;
    f.componentRef.setInput('filas', [{ fila: 2 }, { fila: 3 }]);
    let recibidas: any[] = [];
    c.cambiar.subscribe((filas: any[]) => (recibidas = filas));
    c.mover({ fila: 3 }, -1);
    expect(recibidas.map((f) => f.fila)).toEqual([3, 2]);
  });
});
