import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { ResidenteRequest } from '../../../../models/residente.model';
import { TipoDocumento } from '../../../../models/tipo-documento.model';
import { TextoObligatorio } from '../../../../shared/formularios/restricciones-entrada';

@Component({
  selector: 'app-formulario-residente',
  imports: [TextoObligatorio, FormsModule],
  templateUrl: './formulario-residente.html',
  styleUrl: './formulario-residente.css',
})
export class FormularioResidente {
  readonly datos = model.required<ResidenteRequest>();
  readonly opciones = input<OpcionCatalogo[]>([]);
  readonly tiposDocumento = input<TipoDocumento[]>([]);
  readonly perfiles = input<OpcionCatalogo[]>([]);
  readonly guardando = input(false);
  readonly edicion = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
}
