import { HttpErrorResponse } from '@angular/common/http';
import { mensajeError } from './api-error';

describe('mensajeError', () => {
  it('avisa cuando el backend no responde', () => {
    expect(mensajeError(new HttpErrorResponse({ status: 0 }))).toContain('No pudimos conectar');
  });

  it('une el mensaje del backend con los errores por campo', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        mensaje: 'Revisa los datos del formulario.',
        campos: { nombre: 'Escribe el nombre.' },
      },
    });
    expect(mensajeError(error)).toBe('Revisa los datos del formulario. Escribe el nombre.');
  });

  it('usa un texto genérico para errores desconocidos', () => {
    expect(mensajeError(new Error('x'))).toBe(
      'No se pudo completar la operación. Intenta de nuevo.',
    );
  });
});
