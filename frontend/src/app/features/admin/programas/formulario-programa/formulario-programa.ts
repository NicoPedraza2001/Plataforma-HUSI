import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Universidad } from '../../../../models/universidad.model';
/** Formulario de presentación; el listado conserva la coordinación con el backend. */
@Component({
  selector: 'app-formulario-programa',
  imports: [FormsModule],
  templateUrl: './formulario-programa.html',
  styleUrl: './formulario-programa.css',
})
export class FormularioPrograma {
  readonly nombre = model('');
  readonly universidadId = model<number | null>(null);
  readonly universidades = input<Universidad[]>([]);
  readonly universidadRuta = input<number | null>(null);
  readonly guardando = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
}
