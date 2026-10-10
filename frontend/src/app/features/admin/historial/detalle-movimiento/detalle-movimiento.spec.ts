import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DetalleMovimiento as Movimiento } from '../../../../models/historial.model';
import { HistorialService } from '../../../../services/historial.service';
import { DetalleMovimiento } from './detalle-movimiento';

const ACTUALIZACION: Movimiento = {
  id: 7,
  fecha: '2026-10-10T09:30:00-05:00',
  usuario: 'Administrador Sistema',
  correoUsuario: 'admin@husi.com',
  modulo: 'Universidades',
  accion: 'ACTUALIZÓ',
  registro: 'Universidad del Rosario',
  detalle: 'País: Colombia → Perú',
  datosAnteriores: { Nombre: 'Universidad del Rosario', País: 'Colombia' },
  datosNuevos: { Nombre: 'Universidad del Rosario', País: 'Perú' },
  esImportacion: false,
};

describe('DetalleMovimiento', () => {
  async function crear(movimiento: Movimiento) {
    TestBed.configureTestingModule({
      providers: [{ provide: HistorialService, useValue: { consultar: () => of(movimiento) } }],
    });
    const fixture = TestBed.createComponent(DetalleMovimiento);
    fixture.componentRef.setInput('idMovimiento', movimiento.id);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('should create', async () => {
    const fixture = await crear(ACTUALIZACION);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('muestra quién hizo el cambio y el valor anterior y nuevo', async () => {
    const texto: string = (await crear(ACTUALIZACION)).nativeElement.textContent;
    expect(texto).toContain('Administrador Sistema');
    expect(texto).toContain('Cambios realizados');
    expect(texto).toContain('Colombia');
    expect(texto).toContain('Perú');
  });

  it('una importación muestra su resumen y no registros individuales', async () => {
    const texto: string = (
      await crear({
        ...ACTUALIZACION,
        accion: 'IMPORTÓ',
        modulo: 'Importaciones',
        datosAnteriores: undefined,
        datosNuevos: { 'Residentes creados': 1000, 'Estado de la importación': 'Confirmada' },
        esImportacion: true,
      })
    ).nativeElement.textContent;
    expect(texto).toContain('Resumen de la importación');
    expect(texto).toContain('Residentes creados');
    expect(texto).toContain('1000');
    expect(texto).not.toContain('se canceló');
  });

  it('una importación cancelada lo indica y sigue mostrando lo que se creó', async () => {
    const texto: string = (
      await crear({
        ...ACTUALIZACION,
        accion: 'IMPORTÓ',
        datosAnteriores: undefined,
        datosNuevos: { 'Universidades creadas': 2, 'Estado de la importación': 'Cancelada' },
        esImportacion: true,
      })
    ).nativeElement.textContent;
    expect(texto).toContain('se canceló antes de confirmarse');
    expect(texto).toContain('Universidades creadas');
  });
});
