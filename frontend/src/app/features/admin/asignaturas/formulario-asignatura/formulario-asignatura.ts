import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AsignaturaRequest } from '../../../../models/asignatura.model';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { TextoObligatorio } from '../../../../shared/formularios/restricciones-entrada';

@Component({
  selector: 'app-formulario-asignatura',
  imports: [TextoObligatorio, FormsModule],
  templateUrl: './formulario-asignatura.html',
  styleUrl: './formulario-asignatura.css',
})
export class FormularioAsignatura {
  readonly datos = model.required<AsignaturaRequest>();
  readonly guardando = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
  readonly servicios = input.required<OpcionCatalogo[]>();
}
