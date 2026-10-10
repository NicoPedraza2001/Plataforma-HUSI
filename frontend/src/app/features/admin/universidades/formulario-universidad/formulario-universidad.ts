import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TextoObligatorio } from '../../../../shared/formularios/restricciones-entrada';

/** Formulario de presentación; el listado conserva la coordinación con el backend. */
@Component({
  selector: 'app-formulario-universidad',
  imports: [TextoObligatorio, FormsModule],
  templateUrl: './formulario-universidad.html',
  styleUrl: './formulario-universidad.css',
})
export class FormularioUniversidad {
  readonly nombre = model('');
  readonly pais = model('');
  readonly edicion = input(false);
  readonly guardando = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
}
