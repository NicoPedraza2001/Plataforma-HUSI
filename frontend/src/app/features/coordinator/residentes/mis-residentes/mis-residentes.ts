import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ResidenteACargo, ResidentesACargo } from '../../../../models/coordinador.model';
import { mensajeError } from '../../../../services/api-error';
import { CoordinadorService } from '../../../../services/coordinador.service';
import { SesionActual } from '../../../../services/sesion-actual';
import { Paginacion } from '../../../../shared/paginacion/paginacion';

/** Filas por página: las mismas que traen los listados paginados del backend. */
const TAMANO_PAGINA = 10;

/** Una rotación que termina en estos días o menos se marca como «finaliza pronto». */
const DIAS_AVISO_FIN = 7;

/** Una rotación que empieza en estos días o menos se resalta como «empieza pronto». */
const DIAS_AVISO_INICIO = 7;

const MS_POR_DIA = 86_400_000;

/** Residente con lo que la tabla calcula a partir de la fecha de corte del backend. */
interface FilaResidente extends ResidenteACargo {
  /** Días que faltan hasta la fecha fin (0 = termina hoy). */
  diasRestantes: number;
  /** Porcentaje de la rotación ya cursado, de 0 a 100. */
  avance: number;
  finalizaPronto: boolean;
}

/** Rotación que todavía no empieza, con los días que faltan para su inicio. */
interface FilaProxima extends ResidenteACargo {
  /** Días hasta la fecha de inicio (1 = empieza mañana). */
  diasParaInicio: number;
  iniciaPronto: boolean;
}

/** Fecha ISO `yyyy-MM-dd` como días desde 1970, sin pasar por la zona horaria del navegador. */
function diaIso(fecha: string): number {
  const [anio, mes, dia] = fecha.split('-').map(Number);
  return Date.UTC(anio, mes - 1, dia) / MS_POR_DIA;
}

/** Sin tildes ni mayúsculas, para que «jose» encuentre a «José». */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLocaleLowerCase('es');
}

/** Filas cuyo residente, documento, universidad, programa o asignatura contienen el término. */
function filtrar<T extends ResidenteACargo>(filas: T[], termino: string): T[] {
  const buscado = normalizar(termino.trim());
  if (!buscado) return filas;
  return filas.filter((fila) =>
    normalizar(
      [fila.nombre, fila.documento, fila.universidad, fila.programa, fila.asignatura].join(' '),
    ).includes(buscado),
  );
}

@Component({
  selector: 'app-mis-residentes',
  imports: [Paginacion],
  templateUrl: './mis-residentes.html',
  styleUrl: './mis-residentes.css',
})
export class MisResidentes {
  // Dependencias
  private readonly coordinadorService = inject(CoordinadorService);
  protected readonly perfil = inject(SesionActual).perfil;

  // Estado de la pantalla
  protected readonly datos = signal<ResidentesACargo | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly terminoBusqueda = signal('');
  protected readonly paginaActual = signal(1);

  protected readonly iniciales = computed(() => {
    const perfil = this.perfil();
    if (!perfil) return '';
    return [perfil.nombres, perfil.apellidos]
      .map((parte) => parte.trim().charAt(0))
      .join('')
      .toLocaleUpperCase('es');
  });

  /** Fecha de corte del backend (hoy en Bogotá), escrita para leerla de corrido. */
  protected readonly fechaCorte = computed(() => {
    const fecha = this.datos()?.fecha;
    if (!fecha) return '';
    return new Intl.DateTimeFormat('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC',
    }).format(diaIso(fecha) * MS_POR_DIA);
  });

  protected readonly filas = computed<FilaResidente[]>(() => {
    const datos = this.datos();
    if (!datos) return [];
    const hoy = diaIso(datos.fecha);
    return datos.residentes.map((residente) => {
      const inicio = diaIso(residente.inicio);
      const fin = diaIso(residente.fin);
      const diasRestantes = Math.max(0, fin - hoy);
      const duracion = fin - inicio + 1;
      const cursados = Math.min(duracion, Math.max(0, hoy - inicio + 1));
      return {
        ...residente,
        diasRestantes,
        avance: Math.round((cursados / duracion) * 100),
        finalizaPronto: diasRestantes <= DIAS_AVISO_FIN,
      };
    });
  });

  // Indicadores: todos salen de las rotaciones en curso, nunca de las finalizadas.
  protected readonly totalResidentes = computed(
    () => new Set(this.filas().map((fila) => fila.residenteId)).size,
  );
  protected readonly totalAsignaturas = computed(
    () => new Set(this.filas().map((fila) => fila.asignatura)).size,
  );
  protected readonly totalUniversidades = computed(
    () => new Set(this.filas().map((fila) => fila.universidad)).size,
  );
  protected readonly totalFinalizanPronto = computed(
    () => this.filas().filter((fila) => fila.finalizaPronto).length,
  );

  protected readonly filasFiltradas = computed(() => filtrar(this.filas(), this.terminoBusqueda()));

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filasFiltradas().length / TAMANO_PAGINA)),
  );
  protected readonly indicePrimeraFila = computed(() => (this.paginaActual() - 1) * TAMANO_PAGINA);
  protected readonly filasPagina = computed(() =>
    this.filasFiltradas().slice(this.indicePrimeraFila(), this.indicePrimeraFila() + TAMANO_PAGINA),
  );

  // Próximas rotaciones: las que empiezan después de la fecha de corte.
  protected readonly terminoProximas = signal('');
  protected readonly paginaProximas = signal(1);

  protected readonly proximas = computed<FilaProxima[]>(() => {
    const datos = this.datos();
    if (!datos) return [];
    const hoy = diaIso(datos.fecha);
    return datos.proximos.map((rotacion) => {
      const diasParaInicio = diaIso(rotacion.inicio) - hoy;
      return { ...rotacion, diasParaInicio, iniciaPronto: diasParaInicio <= DIAS_AVISO_INICIO };
    });
  });

  protected readonly proximasFiltradas = computed(() =>
    filtrar(this.proximas(), this.terminoProximas()),
  );
  protected readonly totalPaginasProximas = computed(() =>
    Math.max(1, Math.ceil(this.proximasFiltradas().length / TAMANO_PAGINA)),
  );
  protected readonly indicePrimeraProxima = computed(
    () => (this.paginaProximas() - 1) * TAMANO_PAGINA,
  );
  protected readonly proximasPagina = computed(() =>
    this.proximasFiltradas().slice(
      this.indicePrimeraProxima(),
      this.indicePrimeraProxima() + TAMANO_PAGINA,
    ),
  );

  constructor() {
    this.cargar();
  }

  protected async cargar(): Promise<void> {
    this.cargando.set(true);
    this.error.set('');
    try {
      this.datos.set(await firstValueFrom(this.coordinadorService.listarResidentes()));
      this.paginaActual.set(1);
      this.paginaProximas.set(1);
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected buscar(termino: string): void {
    this.terminoBusqueda.set(termino);
    this.paginaActual.set(1);
  }

  protected cambiarPagina(pagina: number): void {
    this.paginaActual.set(pagina);
  }

  protected buscarProximas(termino: string): void {
    this.terminoProximas.set(termino);
    this.paginaProximas.set(1);
  }

  protected cambiarPaginaProximas(pagina: number): void {
    this.paginaProximas.set(pagina);
  }

  /** `yyyy-MM-dd` → `dd/MM/yyyy`, el formato de fechas del resto del panel. */
  protected fecha(iso: string): string {
    const [anio, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${anio}`;
  }

  protected textoRestante(dias: number): string {
    if (dias === 0) return 'Termina hoy';
    if (dias === 1) return 'Termina mañana';
    return `Faltan ${dias} días`;
  }

  protected textoInicio(dias: number): string {
    if (dias === 1) return 'Empieza mañana';
    return `Empieza en ${dias} días`;
  }
}
