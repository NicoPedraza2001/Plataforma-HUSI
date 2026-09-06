import { Component, computed, signal } from '@angular/core';

/** Estado del residente (RF-13: activar/inactivar). */
export type EstadoResidente = 'ACTIVO' | 'INACTIVO';

/** Fila del listado de residentes. */
export interface Residente {
  id: number;
  nombre: string;
  documento: string;
  universidad: string;
  programa: string;
  estado: EstadoResidente;
  periodo: string;
}

/** Filas por página del listado. */
const TAMANO_PAGINA = 8;

// TODO: reemplazar por los datos que entregue el backend (GET /api/residentes).
const RESIDENTES_DEMO: readonly Residente[] = [
  { id: 1, nombre: 'Juan Carlos Carvajal', documento: 'CC 1014567890', universidad: 'U. Javeriana', programa: 'Medicina Interna', estado: 'ACTIVO', periodo: '2026-2' },
  { id: 2, nombre: 'María José Osorio', documento: 'CC 1023456789', universidad: 'U. Javeriana', programa: 'Pediatría', estado: 'ACTIVO', periodo: '2026-2' },
  { id: 3, nombre: 'Cristian David Castro', documento: 'CE 5748392', universidad: 'U. de los Andes', programa: 'Radiología', estado: 'INACTIVO', periodo: '2026-1' },
  { id: 4, nombre: 'Violeta Bautista', documento: 'CC 1034567890', universidad: 'U. Nacional', programa: 'Cirugía General', estado: 'ACTIVO', periodo: '2026-2' },
  { id: 5, nombre: 'Julián Camilo Zapata', documento: 'CC 1009876543', universidad: 'U. Javeriana', programa: 'Anestesiología', estado: 'ACTIVO', periodo: '2026-2' },
  { id: 6, nombre: 'Daniela Lizarazo', documento: 'CC 1019988776', universidad: 'U. del Rosario', programa: 'Medicina Interna', estado: 'ACTIVO', periodo: '2026-1' },
  { id: 7, nombre: 'María Catalina Fajardo', documento: 'CE 4872319', universidad: 'U. Javeriana', programa: 'Urgencias', estado: 'INACTIVO', periodo: '2026-1' },
  { id: 8, nombre: 'Sebastián Moreno', documento: 'CC 1045678901', universidad: 'U. de La Sabana', programa: 'Oftalmología', estado: 'ACTIVO', periodo: '2026-2' },
  { id: 9, nombre: 'Camilo Rey Peña', documento: 'CC 1050112233', universidad: 'U. Nacional', programa: 'Ortopedia', estado: 'ACTIVO', periodo: '2026-2' },
  { id: 10, nombre: 'Andrea Salazar', documento: 'CC 1061223344', universidad: 'U. de los Andes', programa: 'Dermatología', estado: 'INACTIVO', periodo: '2025-2' },
  { id: 11, nombre: 'Felipe Contreras', documento: 'CC 1072334455', universidad: 'U. del Rosario', programa: 'Cardiología', estado: 'ACTIVO', periodo: '2026-1' },
  { id: 12, nombre: 'Natalia Guzmán', documento: 'CE 6019283', universidad: 'U. Javeriana', programa: 'Ginecología', estado: 'ACTIVO', periodo: '2026-2' },
];

@Component({
  selector: 'app-listado-residentes',
  imports: [],
  templateUrl: './listado-residentes.html',
  styleUrl: './listado-residentes.css',
})
export class ListadoResidentes {
  private readonly residentes = signal<readonly Residente[]>(RESIDENTES_DEMO);

  protected readonly termino = signal('');
  protected readonly universidadSeleccionada = signal('Todas');
  protected readonly programaSeleccionado = signal('Todos');
  protected readonly estadoSeleccionado = signal('Todos');
  protected readonly periodoSeleccionado = signal('Todos');
  protected readonly paginaActual = signal(1);

  /** Opciones de los filtros, derivadas de los datos para no repetir la lista a mano. */
  protected readonly universidades = computed(() => [
    'Todas',
    ...new Set(this.residentes().map((residente) => residente.universidad)),
  ]);

  protected readonly programas = computed(() => [
    'Todos',
    ...new Set(this.residentes().map((residente) => residente.programa)),
  ]);

  protected readonly periodos = computed(() => [
    'Todos',
    ...new Set(this.residentes().map((residente) => residente.periodo)),
  ]);

  protected readonly estados = ['Todos', 'ACTIVO', 'INACTIVO'];

  /** RF-11: filtro combinado por nombre/documento, universidad, programa, estado y periodo. */
  protected readonly filtrados = computed(() => {
    const busqueda = this.termino().trim().toLowerCase();
    const universidad = this.universidadSeleccionada();
    const programa = this.programaSeleccionado();
    const estado = this.estadoSeleccionado();
    const periodo = this.periodoSeleccionado();

    return this.residentes().filter((residente) => {
      if (universidad !== 'Todas' && residente.universidad !== universidad) {
        return false;
      }

      if (programa !== 'Todos' && residente.programa !== programa) {
        return false;
      }

      if (estado !== 'Todos' && residente.estado !== estado) {
        return false;
      }

      if (periodo !== 'Todos' && residente.periodo !== periodo) {
        return false;
      }

      if (
        busqueda &&
        !`${residente.nombre} ${residente.documento}`.toLowerCase().includes(busqueda)
      ) {
        return false;
      }

      return true;
    });
  });

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / TAMANO_PAGINA)),
  );

  protected readonly paginas = computed(() =>
    Array.from({ length: this.totalPaginas() }, (_, indice) => indice + 1),
  );

  protected readonly desde = computed(() => (this.paginaActual() - 1) * TAMANO_PAGINA);

  protected readonly residentesPagina = computed(() =>
    this.filtrados().slice(this.desde(), this.desde() + TAMANO_PAGINA),
  );

  protected buscar(valor: string): void {
    this.termino.set(valor);
    this.paginaActual.set(1);
  }

  protected onUniversidadChange(valor: string): void {
    this.universidadSeleccionada.set(valor);
    this.paginaActual.set(1);
  }

  protected onProgramaChange(valor: string): void {
    this.programaSeleccionado.set(valor);
    this.paginaActual.set(1);
  }

  protected onEstadoChange(valor: string): void {
    this.estadoSeleccionado.set(valor);
    this.paginaActual.set(1);
  }

  protected onPeriodoChange(valor: string): void {
    this.periodoSeleccionado.set(valor);
    this.paginaActual.set(1);
  }

  protected irAPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas()) {
      return;
    }

    this.paginaActual.set(pagina);
  }

  /** Mismos colores de estado que el resto del sistema (ver historial: ACTIVÓ/INACTIVÓ). */
  /** Modificadores compartidos de src/styles/admin.css, como en Usuarios. */
  protected claseBadge(estado: EstadoResidente): string {
    return estado === 'ACTIVO' ? 'badge badge--activo' : 'badge badge--inactivo';
  }

  protected agregarResidente(): void {
    // TODO: navegar al formulario de alta (RF-09) cuando exista /admin/residentes/nuevo.
  }

  protected importar(): void {
    // TODO: conectar con el endpoint de importación masiva de residentes.
  }

  protected editar(residente: Residente): void {
    // TODO: abrir el formulario de edición del residente; desde ahí también se podrá
    // activar/inactivar el registro (RF-13).
  }
}
