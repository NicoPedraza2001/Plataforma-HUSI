import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { UsuarioRequest } from '../../../../models/usuario.model';
import {
  ClaveBcrypt,
  TextoObligatorio,
} from '../../../../shared/formularios/restricciones-entrada';

@Component({
  selector: 'app-formulario-usuario',
  imports: [TextoObligatorio, ClaveBcrypt, FormsModule],
  templateUrl: './formulario-usuario.html',
  styleUrl: './formulario-usuario.css',
})
export class FormularioUsuario {
  readonly datos = model.required<UsuarioRequest>();
  readonly opciones = input<OpcionCatalogo[]>([]);
  readonly guardando = input(false);
  readonly edicion = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();
}
