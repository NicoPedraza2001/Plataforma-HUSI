import { Component, computed, signal } from '@angular/core';
import { Paginacion } from '../../../../shared/paginacion/paginacion';

/** Fila del listado de servicios asistenciales. */
export interface Servicio {
  id: number;
  nombre: string;
  /** Opcional en la base de datos: puede llegar sin diligenciar. */
  descripcion: string | null;
  cupoSimultaneo: number;
  cupoMaximoInstalado: number;
  activo: boolean;
}

/** Opciones del selector de estado. */
export type FiltroEstado = 'todos' | 'activos' | 'inactivos';

/** Filas por página del listado. */
const TAMANO_PAGINA = 10;

// TODO: reemplazar por los datos que entregue el backend.
const SERVICIOS_DEMO: readonly Servicio[] = [
  {
    id: 1,
    nombre: 'Urgencias',
    descripcion: 'Atención inicial del paciente crítico durante las 24 horas.',
    cupoSimultaneo: 4,
    cupoMaximoInstalado: 8,
    activo: true,
  },
  {
    id: 2,
    nombre: 'Medicina general',
    descripcion: null,
    cupoSimultaneo: 3,
    cupoMaximoInstalado: 6,
    activo: true,
  },
  {
    id: 3,
    nombre: 'Cirugía general',
    descripcion: 'Procedimientos quirúrgicos programados y de urgencia.',
    cupoSimultaneo: 2,
    cupoMaximoInstalado: 5,
    activo: true,
  },
  {
    id: 4,
    nombre: 'Pediatría',
    descripcion: 'Manejo integral del paciente pediátrico hospitalizado.',
    cupoSimultaneo: 3,
    cupoMaximoInstalado: 6,
    activo: true,
  },
  {
    id: 5,
    nombre: 'Anestesiología',
    descripcion: null,
    cupoSimultaneo: 2,
    cupoMaximoInstalado: 4,
    activo: false,
  },
  {
    id: 6,
    nombre: 'Medicina interna',
    descripcion: 'Diagnóstico y tratamiento de patología adulta compleja.',
    cupoSimultaneo: 4,
    cupoMaximoInstalado: 8,
    activo: true,
  },
  {
    id: 7,
    nombre: 'Ginecología',
    descripcion: null,
    cupoSimultaneo: 3,
    cupoMaximoInstalado: 5,
    activo: true,
  },
  {
    id: 8,
    nombre: 'Radiología',
    descripcion: 'Apoyo diagnóstico por imágenes para todo el hospital.',
    cupoSimultaneo: 2,
    cupoMaximoInstalado: 4,
    activo: true,
  },
  {
    id: 9,
    nombre: 'Cardiología',
    descripcion: 'Valoración y seguimiento de patología cardiovascular.',
    cupoSimultaneo: 2,
    cupoMaximoInstalado: 4,
    activo: true,
  },
  {
    id: 10,
    nombre: 'Ortopedia',
    descripcion: null,
    cupoSimultaneo: 3,
    cupoMaximoInstalado: 6,
    activo: true,
  },
];

@Component({
  selector: 'app-listado-servicios',
  imports: [Paginacion],
  templateUrl: './listado-servicios.html',
  styleUrl: './listado-servicios.css',
})
export class ListadoServicios {
  private readonly servicios = signal<readonly Servicio[]>(SERVICIOS_DEMO);

  protected readonly termino = signal('');
  protected readonly estado = signal<FiltroEstado>('todos');
  protected readonly paginaActual = signal(1);

  protected readonly filtrados = computed(() => {
    const busqueda = this.termino().trim().toLowerCase();
    const estado = this.estado();

    return this.servicios().filter((servicio) => {
      const coincideEstado =
        estado === 'todos' || (estado === 'activos' ? servicio.activo : !servicio.activo);

      if (!coincideEstado) {
        return false;
      }

      return (
        !busqueda ||
        `${servicio.nombre} ${servicio.descripcion ?? ''}`.toLowerCase().includes(busqueda)
      );
    });
  });

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / TAMANO_PAGINA)),
  );

  /** Índice de la primera fila de la página actual, base para la columna "No.". */
  protected readonly desde = computed(() => (this.paginaActual() - 1) * TAMANO_PAGINA);

  protected readonly serviciosPagina = computed(() =>
    this.filtrados().slice(this.desde(), this.desde() + TAMANO_PAGINA),
  );

  protected buscar(valor: string): void {
    this.termino.set(valor);
    // Al cambiar el filtro la página anterior puede quedar fuera de rango.
    this.paginaActual.set(1);
  }

  protected filtrarPorEstado(valor: string): void {
    this.estado.set(valor as FiltroEstado);
    this.paginaActual.set(1);
  }

  protected irAPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas()) {
      return;
    }

    this.paginaActual.set(pagina);
  }

  protected alternarEstado(servicio: Servicio): void {
    // TODO: conectar con el servicio de servicios del backend.
  }

  protected editar(servicio: Servicio): void {
    // TODO: abrir el formulario de edición del servicio.
  }
}
