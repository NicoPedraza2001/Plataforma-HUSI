import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ResidenteEnEspera, RotacionGestion } from '../../../../../models/rotacion.model';
import { mensajeError } from '../../../../../services/api-error';
import { RotacionService } from '../../../../../services/rotacion.service';

/** Ofrece trasladar a esta rotación a quienes esperan cupo en otras del mismo servicio. */
@Component({
  selector: 'app-espera-servicio',
  imports: [DatePipe],
  templateUrl: './espera-servicio.html',
  styleUrl: './espera-servicio.css',
})
export class EsperaServicio implements OnInit {
  // Dependencias
  private readonly rotacionService = inject(RotacionService);

  // Estado de la pantalla
  readonly rotacion = input.required<RotacionGestion>();
  readonly trasladados = output<number>();
  protected readonly filas = signal<ResidenteEnEspera[]>([]);
  protected readonly cuposDisponibles = signal(0);
  protected readonly seleccionados = signal<ReadonlySet<string>>(new Set());
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly excedeCupos = computed(
    () => this.seleccionados().size > this.cuposDisponibles(),
  );

  ngOnInit() {
    this.cargar();
  }

  /** Relee cupos y espera; el listado de residentes lo llama tras cada cambio. */
  async cargar() {
    this.cargando.set(true);
    this.error.set('');
    try {
      const [rotacion, filas] = await Promise.all([
        firstValueFrom(this.rotacionService.consultar(this.rotacion())),
        firstValueFrom(this.rotacionService.listarEsperaDelServicio(this.rotacion())),
      ]);
      this.cuposDisponibles.set(rotacion.cupoDisponible);
      this.filas.set(filas);
      // Preselecciona por orden de llegada, sin quienes tienen cruce de fechas.
      this.seleccionados.set(
        new Set(
          filas
            .filter((residente) => !residente.cruce)
            .slice(0, rotacion.cupoDisponible)
            .map((residente) => this.claveDe(residente)),
        ),
      );
    } catch (e) {
      this.error.set('No se pudo consultar la lista de espera del servicio. ' + mensajeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected claveDe(residente: ResidenteEnEspera) {
    return `${residente.residenteId}|${residente.asignaturaId}|${residente.inicio}|${residente.fin}`;
  }

  protected alternarSeleccion(residente: ResidenteEnEspera) {
    const seleccion = new Set(this.seleccionados());
    const clave = this.claveDe(residente);
    if (!seleccion.delete(clave)) seleccion.add(clave);
    this.seleccionados.set(seleccion);
  }

  protected async trasladar() {
    const elegidos = this.filas().filter((residente) =>
      this.seleccionados().has(this.claveDe(residente)),
    );
    if (!elegidos.length || this.excedeCupos() || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      await firstValueFrom(
        this.rotacionService.trasladarEspera(
          this.rotacion(),
          elegidos.map(({ residenteId, asignaturaId, inicio, fin }) => ({
            residenteId,
            asignaturaId,
            inicio,
            fin,
          })),
        ),
      );
      this.trasladados.emit(elegidos.length);
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
