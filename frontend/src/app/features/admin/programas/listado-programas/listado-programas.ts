import { Component, computed, signal } from '@angular/core';

/** Fila del listado de programas académicos. */
export interface Programa {
  id: number;
  nombre: string;
  categoria: string;
  asignatura: string;
}

/** Filas por página del listado. */
const TAMANO_PAGINA = 10;

// TODO: reemplazar por los datos que entregue el backend.
const PROGRAMAS_DEMO: readonly Programa[] = [
  { id: 1, nombre: 'Especializacion Radiologia', categoria: 'Docencia Servicio', asignatura: 'Radiologia Integral II' },
  { id: 2, nombre: 'Especializacion Anestesiologia', categoria: 'Docencia Servicio', asignatura: 'Anestesia Clinica I' },
  { id: 3, nombre: 'Especializacion Pediatria', categoria: 'Docencia Servicio', asignatura: 'Pediatria General III' },
  { id: 4, nombre: 'Especializacion Cirugia General', categoria: 'Docencia Servicio', asignatura: 'Cirugia Basica II' },
  { id: 5, nombre: 'Especializacion Medicina Interna', categoria: 'Docencia Servicio', asignatura: 'Clinica Medica IV' },
  { id: 6, nombre: 'Especializacion Ginecologia', categoria: 'Docencia Servicio', asignatura: 'Obstetricia II' },
  { id: 7, nombre: 'Especializacion Ortopedia', categoria: 'Convenio Docente', asignatura: 'Trauma y Ortopedia I' },
  { id: 8, nombre: 'Especializacion Dermatologia', categoria: 'Convenio Docente', asignatura: 'Dermatologia Clinica II' },
  { id: 9, nombre: 'Especializacion Neurologia', categoria: 'Convenio Docente', asignatura: 'Neurologia Aplicada III' },
  { id: 10, nombre: 'Especializacion Cardiologia', categoria: 'Convenio Docente', asignatura: 'Cardiologia Integral I' },
];

@Component({
  selector: 'app-listado-programas',
  imports: [],
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
      `${programa.nombre} ${programa.categoria} ${programa.asignatura}`
        .toLowerCase()
        .includes(busqueda),
    );
  });

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / TAMANO_PAGINA)),
  );

  protected readonly paginas = computed(() =>
    Array.from({ length: this.totalPaginas() }, (_, indice) => indice + 1),
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
