import { Component, computed, effect, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Universidad } from '../../../../models/universidad.model';
import { TextoObligatorio } from '../../../../shared/formularios/restricciones-entrada';
import {
  buscarConvenio,
  buscarErrorDeConvenioSegunPais,
  convenioSegunPais,
  esUniversidadPropia,
  PROGRAMAS_DE_CONVENIO,
} from '../programas-de-convenio';

/** Formulario de presentación; el listado conserva la coordinación con el backend. */
@Component({
  selector: 'app-formulario-programa',
  imports: [TextoObligatorio, FormsModule],
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

  protected readonly programasDeConvenio = PROGRAMAS_DE_CONVENIO;
  protected readonly universidadSeleccionada = computed(
    () =>
      this.universidades().find((universidad) => universidad.id === this.universidadId()) ?? null,
  );
  /** Universidad externa a la Javeriana: su programa solo puede ser uno de los dos convenios. */
  protected readonly soloAdmiteConvenios = computed(() => {
    const universidad = this.universidadSeleccionada();
    return !!universidad && !esUniversidadPropia(universidad.nombre);
  });
  /** Mensaje que impide guardar un «Convenio Internacional» en una universidad nacional. */
  protected readonly errorDeConvenio = computed(() => {
    const universidad = this.universidadSeleccionada();
    return universidad && this.soloAdmiteConvenios()
      ? buscarErrorDeConvenioSegunPais(this.nombre(), universidad)
      : null;
  });

  constructor() {
    // Al elegir una universidad externa, el nombre pasa al convenio que le corresponde por país
    // (o al convenio ya escrito, con su ortografía exacta) para que la lista lo muestre elegido.
    effect(() => {
      const universidad = this.universidadSeleccionada();
      if (!universidad || !this.soloAdmiteConvenios()) return;
      const convenioElegido = buscarConvenio(this.nombre()) ?? convenioSegunPais(universidad.pais);
      if (convenioElegido !== this.nombre()) this.nombre.set(convenioElegido);
    });
  }
}
