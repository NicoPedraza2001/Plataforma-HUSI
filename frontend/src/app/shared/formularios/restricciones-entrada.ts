import { Directive, forwardRef } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';
import { claveBcrypt, enteroBaseDatos, textoNoVacio } from './validadores';

@Directive({
  selector: 'input[required][ngModel]:not([type=password]),textarea[required][ngModel]',
  providers: [
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => TextoObligatorio), multi: true },
  ],
})
export class TextoObligatorio implements Validator {
  validate(control: AbstractControl): ValidationErrors | null {
    return textoNoVacio(control);
  }
}

@Directive({
  selector: 'input[type=number][ngModel]',
  providers: [
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => EnteroBaseDatos), multi: true },
  ],
})
export class EnteroBaseDatos implements Validator {
  validate(control: AbstractControl): ValidationErrors | null {
    return enteroBaseDatos(control);
  }
}

@Directive({
  selector: 'input[type=password][ngModel],input[claveBcrypt][ngModel]',
  providers: [{ provide: NG_VALIDATORS, useExisting: forwardRef(() => ClaveBcrypt), multi: true }],
})
export class ClaveBcrypt implements Validator {
  validate(control: AbstractControl): ValidationErrors | null {
    return claveBcrypt(control);
  }
}
