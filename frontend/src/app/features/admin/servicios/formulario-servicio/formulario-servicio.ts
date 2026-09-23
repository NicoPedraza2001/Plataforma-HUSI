import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ServicioRequest } from '../../../../models/servicio.model';
@Component({
  selector: 'app-formulario-servicio',
  imports: [FormsModule],
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
    const d = this.datos();
    return (
      d.cupoSimultaneo !== null &&
      d.cupoMaximoInstalado !== null &&
      Number.isInteger(d.cupoSimultaneo) &&
      Number.isInteger(d.cupoMaximoInstalado) &&
      d.cupoSimultaneo >= 0 &&
      d.cupoMaximoInstalado >= d.cupoSimultaneo
    );
  }
}
