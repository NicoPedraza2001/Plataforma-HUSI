import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CoordinadorRequest } from '../../../../models/coordinador.model';
@Component({
  selector: 'app-formulario-coordinador',
  imports: [FormsModule],
  templateUrl: './formulario-coordinador.html',
  styleUrl: './formulario-coordinador.css',
})
export class FormularioCoordinador {
  readonly datos = model.required<CoordinadorRequest>();
  readonly guardando = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
  readonly servicios = input.required<OpcionCatalogo[]>();
  readonly usuarios = input.required<OpcionCatalogo[]>();
}
