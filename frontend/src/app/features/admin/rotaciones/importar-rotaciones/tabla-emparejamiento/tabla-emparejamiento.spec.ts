import { TestBed } from '@angular/core/testing';
import { GrupoEmparejamiento } from '../emparejamiento';
import { AsociacionElegida, TablaEmparejamiento } from './tabla-emparejamiento';

function crearGrupo(cambios: Partial<GrupoEmparejamiento>): GrupoEmparejamiento {
  return {
    clave: 'javeriana',
    valorEnExcel: 'Javeriana',
    valoresOriginales: [],
    contexto: 'Colombia',
    contextoId: null,
    numerosFila: [2, 3],
    estado: 'nuevo',
    registro: null,
    observaciones: [],
    sugerencias: [],
    opciones: [],
    sePuedeCrear: true,
    ...cambios,
  };
}

describe('TablaEmparejamiento', () => {
  function crearTabla(grupos: GrupoEmparejamiento[]) {
    const fixture = TestBed.createComponent(TablaEmparejamiento);
    fixture.componentRef.setInput('grupos', grupos);
    fixture.componentRef.setInput('nombreEntidad', 'universidad');
    fixture.componentRef.setInput('tituloContexto', 'País');
    fixture.detectChanges();
    return { fixture, tabla: fixture.componentInstance as any };
  }

  it('should create', () => {
    expect(crearTabla([]).tabla).toBeTruthy();
  });

  it('muestra primero lo pendiente y filtra por estado', () => {
    const { tabla } = crearTabla([
      crearGrupo({ clave: 'a', estado: 'encontrado' }),
      crearGrupo({ clave: 'b', estado: 'nuevo' }),
    ]);
    expect(
      tabla.filtro.elementosVisibles().map((grupo: GrupoEmparejamiento) => grupo.clave),
    ).toEqual(['b', 'a']);
    tabla.filtro.cambiarFiltro('encontrado');
    expect(tabla.filtro.elementosVisibles()).toHaveLength(1);
  });

  it('emite la asociación al pulsar una sugerencia', () => {
    const sugerencia = { id: 7, nombre: 'Pontificia Universidad Javeriana', estado: 'Activo' };
    const { fixture, tabla } = crearTabla([crearGrupo({ sugerencias: [sugerencia] })]);
    let asociacion: AsociacionElegida | null = null;
    tabla.asociar.subscribe((elegida: AsociacionElegida) => (asociacion = elegida));
    (fixture.nativeElement.querySelector('.sugerencia') as HTMLButtonElement).click();
    expect(asociacion!.registroId).toBe(7);
  });
});
