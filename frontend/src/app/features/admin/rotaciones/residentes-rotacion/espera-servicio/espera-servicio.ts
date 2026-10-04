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
  private readonly api = inject(RotacionService);

  // Estado de la pantalla
  readonly rotacion = input.required<RotacionGestion>();
  readonly trasladados = output<number>();
  protected readonly filas = signal<ResidenteEnEspera[]>([]);
  protected readonly cupos = signal(0);
  protected readonly seleccion = signal<ReadonlySet<string>>(new Set());
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly excede = computed(() => this.seleccion().size > this.cupos());

  ngOnInit() {
    this.cargar();
  }

  /** Relee cupos y espera; el listado de residentes lo llama tras cada cambio. */
  async cargar() {
    this.cargando.set(true);
    this.error.set('');
    try {
      const [rotacion, filas] = await Promise.all([
        firstValueFrom(this.api.consultar(this.rotacion())),
        firstValueFrom(this.api.espera(this.rotacion())),
      ]);
      this.cupos.set(rotacion.cupoDisponible);
      this.filas.set(filas);
      // Preselecciona por orden de llegada, sin quienes tienen cruce de fechas.
      this.seleccion.set(
        new Set(
          filas
            .filter((f) => !f.cruce)
            .slice(0, rotacion.cupoDisponible)
            .map((f) => this.clave(f)),
        ),
      );
    } catch (e) {
      this.error.set('No se pudo consultar la lista de espera del servicio. ' + mensajeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected clave(f: ResidenteEnEspera) {
    return `${f.residenteId}|${f.asignaturaId}|${f.inicio}|${f.fin}`;
  }

  protected alternar(f: ResidenteEnEspera) {
    const seleccion = new Set(this.seleccion());
    const clave = this.clave(f);
    if (!seleccion.delete(clave)) seleccion.add(clave);
    this.seleccion.set(seleccion);
  }

  protected async trasladar() {
    const elegidos = this.filas().filter((f) => this.seleccion().has(this.clave(f)));
    if (!elegidos.length || this.excede() || this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    try {
      await firstValueFrom(
        this.api.trasladarEspera(
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
