import { Component, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

/**
 * Tipos de acción auditados por el módulo de historial (RF-23, RF-24, M06).
 * El texto coincide con el badge que se pinta en la tabla.
 */
export type AccionHistorial = 'ACTUALIZÓ' | 'CREÓ' | 'INACTIVÓ' | 'ACTIVÓ' | 'FINALIZÓ' | 'INGRESÓ';

/** Fila del historial de cambios / auditoría. */
export interface RegistroHistorial {
  id: number;
  fecha: Date;
  usuario: string;
  modulo: string;
  accion: AccionHistorial;
  registro: string;
  detalle: string;
}

/** Filas por página del listado. */
const TAMANO_PAGINA = 8;

/** Opciones fijas del filtro de acción; el orden refleja el de la pantalla de Figma. */
const ACCIONES: readonly AccionHistorial[] = [
  'ACTUALIZÓ',
  'CREÓ',
  'INACTIVÓ',
  'ACTIVÓ',
  'FINALIZÓ',
  'INGRESÓ',
];

// TODO: reemplazar por los datos que entregue el backend (GET /api/historial).
const REGISTROS_DEMO: readonly RegistroHistorial[] = [
  { id: 1, fecha: new Date(2026, 6, 29, 9, 12), usuario: 'Laura Martínez', modulo: 'Usuarios', accion: 'ACTUALIZÓ', registro: 'Gabriela Pérez', detalle: 'Cambió el rol de Residente a Doctor.' },
  { id: 2, fecha: new Date(2026, 6, 29, 8, 47), usuario: 'Camila Torres', modulo: 'Rotaciones', accion: 'CREÓ', registro: 'ROT-2026-184', detalle: 'Asignó residente a Urgencias.' },
  { id: 3, fecha: new Date(2026, 6, 28, 16, 20), usuario: 'Laura Martínez', modulo: 'Servicios', accion: 'INACTIVÓ', registro: 'Anestesiología', detalle: 'El servicio quedó inactivo.' },
  { id: 4, fecha: new Date(2026, 6, 28, 14, 3), usuario: 'Camila Torres', modulo: 'Residentes', accion: 'ACTUALIZÓ', registro: 'Juan Carvajal', detalle: 'Actualizó universidad y programa.' },
  { id: 5, fecha: new Date(2026, 6, 27, 11, 35), usuario: 'Laura Martínez', modulo: 'Usuarios', accion: 'ACTIVÓ', registro: 'Andrés Gómez', detalle: 'Reactivó el acceso del usuario.' },
  { id: 6, fecha: new Date(2026, 6, 26, 15, 18), usuario: 'Camila Torres', modulo: 'Rotaciones', accion: 'FINALIZÓ', registro: 'ROT-2026-159', detalle: 'Marcó la rotación como finalizada.' },
  { id: 7, fecha: new Date(2026, 6, 25, 10, 2), usuario: 'Soporte técnico', modulo: 'Seguridad', accion: 'INGRESÓ', registro: 'Sesión administrativa', detalle: 'Inicio de sesión exitoso.' },
  { id: 8, fecha: new Date(2026, 6, 24, 17, 41), usuario: 'Laura Martínez', modulo: 'Universidades', accion: 'CREÓ', registro: 'Universidad del Norte', detalle: 'Registró una universidad externa.' },
  { id: 9, fecha: new Date(2026, 6, 23, 9, 55), usuario: 'Camila Torres', modulo: 'Programa', accion: 'CREÓ', registro: 'Especialización Cardiología', detalle: 'Creó un nuevo programa académico.' },
  { id: 10, fecha: new Date(2026, 6, 22, 13, 27), usuario: 'Laura Martínez', modulo: 'Residentes', accion: 'INACTIVÓ', registro: 'Camilo Rey', detalle: 'Desactivó el registro del residente.' },
  { id: 11, fecha: new Date(2026, 6, 21, 8, 10), usuario: 'Soporte técnico', modulo: 'Seguridad', accion: 'INGRESÓ', registro: 'Sesión administrativa', detalle: 'Inicio de sesión exitoso.' },
  { id: 12, fecha: new Date(2026, 6, 20, 18, 33), usuario: 'Camila Torres', modulo: 'Usuarios', accion: 'ACTUALIZÓ', registro: 'María Fernanda Ruiz', detalle: 'Actualizó el correo electrónico.' },
];

@Component({
  selector: 'app-historial',
  imports: [DatePipe],
  templateUrl: './historial.html',
  styleUrl: './historial.css',
})
export class Historial {
  private readonly registros = signal<readonly RegistroHistorial[]>(REGISTROS_DEMO);

  protected readonly termino = signal('');
  protected readonly moduloSeleccionado = signal('Todos');
  protected readonly accionSeleccionada = signal('Todas');
  protected readonly fechaInicial = signal('');
  protected readonly fechaFinal = signal('');
  protected readonly paginaActual = signal(1);

  /** Módulos presentes en los datos, para poblar el filtro sin repetir la lista a mano. */
  protected readonly modulos = computed(() => [
    'Todos',
    ...new Set(this.registros().map((registro) => registro.modulo)),
  ]);

  protected readonly acciones = ['Todas', ...ACCIONES];

  protected readonly filtrados = computed(() => {
    const busqueda = this.termino().trim().toLowerCase();
    const modulo = this.moduloSeleccionado();
    const accion = this.accionSeleccionada();
    const desde = this.fechaInicial();
    const hasta = this.fechaFinal();

    return this.registros().filter((registro) => {
      if (modulo !== 'Todos' && registro.modulo !== modulo) {
        return false;
      }

      if (accion !== 'Todas' && registro.accion !== accion) {
        return false;
      }

      const fechaRegistro = this.isoDate(registro.fecha);

      if (desde && fechaRegistro < desde) {
        return false;
      }

      if (hasta && fechaRegistro > hasta) {
        return false;
      }

      if (
        busqueda &&
        !`${registro.usuario} ${registro.registro} ${registro.detalle}`
          .toLowerCase()
          .includes(busqueda)
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

  private readonly desde = computed(() => (this.paginaActual() - 1) * TAMANO_PAGINA);

  protected readonly registrosPagina = computed(() =>
    this.filtrados().slice(this.desde(), this.desde() + TAMANO_PAGINA),
  );

  protected buscar(valor: string): void {
    this.termino.set(valor);
    this.paginaActual.set(1);
  }

  protected onModuloChange(valor: string): void {
    this.moduloSeleccionado.set(valor);
    this.paginaActual.set(1);
  }

  protected onAccionChange(valor: string): void {
    this.accionSeleccionada.set(valor);
    this.paginaActual.set(1);
  }

  protected onFechaInicialChange(valor: string): void {
    this.fechaInicial.set(valor);
    this.paginaActual.set(1);
  }

  protected onFechaFinalChange(valor: string): void {
    this.fechaFinal.set(valor);
    this.paginaActual.set(1);
  }

  protected irAPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas()) {
      return;
    }

    this.paginaActual.set(pagina);
  }

  /**
   * Modificadores compartidos de src/styles/admin.css, como en Usuarios:
   * verde para lo que da de alta, rojo para lo que da de baja y azul para
   * la acción neutra, que deja constancia sin ser un desenlace.
   */
  protected claseBadge(accion: AccionHistorial): string {
    switch (accion) {
      case 'CREÓ':
      case 'ACTIVÓ':
        return 'badge badge--activo';
      case 'INACTIVÓ':
        return 'badge badge--inactivo';
      default:
        return 'badge badge--info';
    }
  }

  protected exportar(): void {
    // TODO: conectar con el endpoint de exportación del historial (RNF-04).
  }

  private isoDate(fecha: Date): string {
    const y = fecha.getFullYear();
    const m = String(fecha.getMonth() + 1).padStart(2, '0');
    const d = String(fecha.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
