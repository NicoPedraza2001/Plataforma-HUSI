import { TestBed } from '@angular/core/testing';
import { ConfirmarExclusion } from './confirmar-exclusion';

describe('ConfirmarExclusion', () => {
  it('should create', () => {
    const fixture = TestBed.createComponent(ConfirmarExclusion);
    fixture.componentRef.setInput('descripcion', 'la universidad «Javeriana»');
    fixture.componentRef.setInput('numerosFila', [2, 3]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Se quitarán 2 filas');
  });
});
