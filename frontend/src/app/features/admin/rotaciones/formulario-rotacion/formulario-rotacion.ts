import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RotacionGestion, CatalogosRotacion } from '../../../../models/rotacion.model';
/** Formulario de presentación; el listado conserva la coordinación con el backend. */
@Component({
  selector: 'app-formulario-rotacion',
  imports: [FormsModule],
  templateUrl: './formulario-rotacion.html',
  styleUrl: './formulario-rotacion.css',
})
export class FormularioRotacion {
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
}
