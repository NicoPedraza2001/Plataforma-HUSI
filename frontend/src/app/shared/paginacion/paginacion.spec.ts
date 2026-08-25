import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Paginacion } from './paginacion';

describe('Paginacion', () => {
  let fixture: ComponentFixture<Paginacion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Paginacion],
    }).compileComponents();

    fixture = TestBed.createComponent(Paginacion);
  });

  /** Lo que ve el usuario entre las dos flechas: números y cortes, en orden. */
  function etiquetas(): string[] {
    const nodos = fixture.nativeElement.querySelectorAll('.pag--num, .pag--corte');

    return Array.from(nodos, (nodo) => (nodo as HTMLElement).textContent?.trim() ?? '');
  }

  function pintar(paginaActual: number, totalPaginas: number): void {
    fixture.componentRef.setInput('paginaActual', paginaActual);
    fixture.componentRef.setInput('totalPaginas', totalPaginas);
    fixture.detectChanges();
  }

  it('muestra todas las páginas mientras quepan sin recortar', () => {
    pintar(3, 7);

    expect(etiquetas()).toEqual(['1', '2', '3', '4', '5', '6', '7']);
  });

  it('acota los botones por muchas páginas que haya', () => {
    pintar(30, 63);

    expect(etiquetas()).toEqual(['1', '…', '29', '30', '31', '…', '63']);
  });

  it('no abre corte del lado donde la ventana ya toca el extremo', () => {
    pintar(1, 63);

    expect(etiquetas()).toEqual(['1', '2', '…', '63']);
  });

  it('emite la página pedida', () => {
    pintar(2, 5);

    const pedidas: number[] = [];
    fixture.componentInstance.cambio.subscribe((pagina) => pedidas.push(pagina));

    const botones = fixture.nativeElement.querySelectorAll('.pag--num');
    (botones[4] as HTMLButtonElement).click();

    expect(pedidas).toEqual([5]);
  });

  it('ignora la página en la que ya se está', () => {
    pintar(2, 5);

    const pedidas: number[] = [];
    fixture.componentInstance.cambio.subscribe((pagina) => pedidas.push(pagina));

    const botones = fixture.nativeElement.querySelectorAll('.pag--num');
    (botones[1] as HTMLButtonElement).click();

    expect(pedidas).toEqual([]);
  });
});
