import { Component, computed, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CatalogosRotacion, RotacionGestion } from '../../../../models/rotacion.model';
import { TextoObligatorio } from '../../../../shared/formularios/restricciones-entrada';
import { hoyBogota } from '../hoy';

/** Formulario de presentación; el listado conserva la coordinación con el backend. */
@Component({
  selector: 'app-formulario-rotacion',
  imports: [TextoObligatorio, FormsModule],
  templateUrl: './formulario-rotacion.html',
  styleUrl: './formulario-rotacion.css',
})
export class FormularioRotacion {
  // Estado de la pantalla y datos del formulario
  readonly asignaturaId = model(0);
  readonly inicio = model('');
  readonly fin = model('');
  readonly edicion = input<RotacionGestion | null>(null);
  readonly asignaturas = input<CatalogosRotacion['asignaturas']>([]);
  readonly cargandoCatalogos = input(false);
  readonly errorCatalogos = input('');
  readonly reintentar = output<void>();
  readonly guardando = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
  protected readonly hoy = hoyBogota();
  /** Al crear o mover fechas, la rotación no puede empezar antes de hoy. */
  protected readonly inicioPasado = computed(() => {
    const actual = this.edicion();
    const sinCambios = actual && this.inicio() === actual.inicio && this.fin() === actual.fin;
    return !sinCambios && !!this.inicio() && this.inicio() < this.hoy;
  });
  protected readonly puedeGuardar = computed(
    () =>
      !this.guardando() &&
      this.asignaturaId() > 0 &&
      this.inicio() >= '1900-01-01' &&
      !this.inicioPasado() &&
      this.fin() <= '9999-12-31' &&
      this.fin() >= this.inicio() &&
      !this.edicion()?.relaciones &&
      !this.cargandoCatalogos() &&
      !this.errorCatalogos(),
  );
}
