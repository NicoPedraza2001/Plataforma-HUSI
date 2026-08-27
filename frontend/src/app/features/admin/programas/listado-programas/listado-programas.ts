import { Component, computed, signal } from '@angular/core';
import { Paginacion } from '../../../../shared/paginacion/paginacion';

/** Fila del listado de programas académicos. */
export interface Programa {
  id: number;
  nombre: string;
  asignatura: string;
}

/** Filas por página del listado. */
const TAMANO_PAGINA = 10;

// TODO: reemplazar por los datos que entregue el backend.
const PROGRAMAS_DEMO: readonly Programa[] = [
  { id: 1, nombre: 'Especializacion Ortopedia', asignatura: 'Trauma y Ortopedia I' },
  { id: 2, nombre: 'Especializacion Dermatologia', asignatura: 'Dermatologia Clinica II' },
  { id: 3, nombre: 'Especializacion Neurologia', asignatura: 'Neurologia Aplicada III' },
  { id: 4, nombre: 'Especializacion Cardiologia', asignatura: 'Cardiologia Integral I' },
];

@Component({
  selector: 'app-listado-programas',
  imports: [Paginacion],
  templateUrl: './listado-programas.html',
  styleUrl: './listado-programas.css',
})
export class ListadoProgramas {
  private readonly programas = signal<readonly Programa[]>(PROGRAMAS_DEMO);

  protected readonly termino = signal('');
  protected readonly paginaActual = signal(1);

  protected readonly filtrados = computed(() => {
    const busqueda = this.termino().trim().toLowerCase();

    if (!busqueda) {
      return this.programas();
    }

    return this.programas().filter((programa) =>
      `${programa.nombre} ${programa.asignatura}`.toLowerCase().includes(busqueda),
    );
  });

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / TAMANO_PAGINA)),
  );

  /** Índice de la primera fila de la página actual, base para la columna "No". */
  protected readonly desde = computed(() => (this.paginaActual() - 1) * TAMANO_PAGINA);

  protected readonly programasPagina = computed(() =>
    this.filtrados().slice(this.desde(), this.desde() + TAMANO_PAGINA),
  );

  protected buscar(valor: string): void {
    this.termino.set(valor);
    // Al cambiar el filtro la página anterior puede quedar fuera de rango.
    this.paginaActual.set(1);
  }

  protected irAPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas()) {
      return;
    }

    this.paginaActual.set(pagina);
  }

  protected eliminar(programa: Programa): void {
    // TODO: conectar con el servicio de programas del backend.
  }

  protected editar(programa: Programa): void {
    // TODO: abrir el formulario de edición del programa.
  }
}
