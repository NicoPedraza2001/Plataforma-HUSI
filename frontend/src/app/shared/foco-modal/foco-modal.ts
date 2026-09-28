import {
  afterNextRender,
  Directive,
  ElementRef,
  HostListener,
  inject,
  OnDestroy,
} from '@angular/core';
@Directive({ selector: '[appFocoModal]' })
export class FocoModal implements OnDestroy {
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly anterior = document.activeElement as HTMLElement | null;

  constructor() {
    afterNextRender(() => this.controles()[0]?.focus());
  }

  @HostListener('keydown', ['$event']) tabular(event: KeyboardEvent) {
    if (event.key !== 'Tab') return;
    const elementos = this.controles();
    const primero = elementos[0],
      ultimo = elementos.at(-1);
    if (!primero) {
      event.preventDefault();
      return;
    }
    if (event.shiftKey && document.activeElement === primero) {
      event.preventDefault();
      ultimo?.focus();
    } else if (!event.shiftKey && document.activeElement === ultimo) {
      event.preventDefault();
      primero.focus();
    }
  }

  ngOnDestroy() {
    this.anterior?.focus();
  }

  private controles() {
    return Array.from(
      this.elemento.querySelectorAll<HTMLElement>(
        'input:not([disabled]),select:not([disabled]),button:not([disabled]),[tabindex="0"]',
      ),
    );
  }
}
