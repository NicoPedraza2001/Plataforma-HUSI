import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { OpcionCatalogo } from '../../../../models/opcion-catalogo.model';
import { ROL_COORDINADOR } from '../../../../models/sesion.model';
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
  /** Servicios que puede coordinar el usuario; solo se piden con el rol Coordinador. */
  readonly servicios = input<OpcionCatalogo[]>([]);
  readonly guardando = input(false);
  readonly edicion = input(false);
  readonly errorFormulario = input('');
  readonly guardar = output<void>();
  readonly cancelar = output<void>();

  /** El coordinador necesita un servicio: en su perfil verá los residentes que rotan en él. */
  protected esCoordinador(): boolean {
    const rolElegido = this.opciones().find((opcion) => opcion.id === this.datos().rolId);
    return rolElegido?.nombre === ROL_COORDINADOR;
  }

  /** Un servicio elegido no se envía si el rol final no es Coordinador. */
  protected enviar() {
    if (!this.esCoordinador()) this.datos().servicioId = null;
    this.guardar.emit();
  }
}
