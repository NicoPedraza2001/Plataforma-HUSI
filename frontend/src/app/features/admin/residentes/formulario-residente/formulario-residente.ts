import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ResidenteRequest } from '../../../../models/residente.model';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
@Component({
  selector: 'app-formulario-residente',
  imports: [FormsModule],
  templateUrl: './formulario-residente.html',
  styleUrl: './formulario-residente.css',
})
export class FormularioResidente {
  readonly datos = model.required<ResidenteRequest>();
  readonly opciones = input<OpcionCatalogo[]>([]);
  readonly guardando = input(false);
  readonly edicion = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
}
