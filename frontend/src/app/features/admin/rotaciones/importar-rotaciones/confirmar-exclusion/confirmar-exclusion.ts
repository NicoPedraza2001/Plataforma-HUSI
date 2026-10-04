import { Component, input, output } from '@angular/core';
import { FocoModal } from '../../../../../shared/foco-modal/foco-modal';
import { describirFilas } from '../emparejamiento';

/** Diálogo que confirma quitar filas de la importación. Lo comparten los pasos de emparejamiento. */
@Component({
  selector: 'app-confirmar-exclusion',
  imports: [FocoModal],
  templateUrl: './confirmar-exclusion.html',
  styleUrl: './confirmar-exclusion.css',
})
export class ConfirmarExclusion {
  /** Qué se excluye, p. ej. «la universidad «Javeriana»». */
  readonly descripcion = input.required<string>();
  readonly numerosFila = input.required<number[]>();
  readonly confirmar = output<void>();
  readonly cancelar = output<void>();

  protected readonly describirFilas = describirFilas;
}
