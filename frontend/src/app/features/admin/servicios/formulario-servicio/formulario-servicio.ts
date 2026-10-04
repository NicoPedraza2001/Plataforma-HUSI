import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ServicioRequest } from '../../../../models/servicio.model';
import {
  EnteroBaseDatos,
  TextoObligatorio,
} from '../../../../shared/formularios/restricciones-entrada';

@Component({
  selector: 'app-formulario-servicio',
  imports: [TextoObligatorio, EnteroBaseDatos, FormsModule],
  templateUrl: './formulario-servicio.html',
  styleUrl: './formulario-servicio.css',
})
export class FormularioServicio {
  readonly datos = model.required<ServicioRequest>();
  readonly guardando = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();

  protected cuposValidos() {
    const servicio = this.datos();
    return (
      servicio.cupoSimultaneo !== null &&
      servicio.cupoMaximoInstalado !== null &&
      Number.isInteger(servicio.cupoSimultaneo) &&
      Number.isInteger(servicio.cupoMaximoInstalado) &&
      servicio.cupoSimultaneo >= 0 &&
      servicio.cupoMaximoInstalado >= servicio.cupoSimultaneo
    );
  }
}
