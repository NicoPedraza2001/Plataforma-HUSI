import { Component, computed, inject, signal } from '@angular/core';
import { FocoModal } from '../foco-modal';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { ProgramasApi, Programa, Universidad, mensajeError } from '../programas-api';
import { ImportarProgramas } from '../importar-programas/importar-programas';
@Component({
  selector: 'app-listado-programas',
  imports: [Paginacion, FormsModule, ImportarProgramas, FocoModal],
  templateUrl: './listado-programas.html',
  styleUrl: './listado-programas.css',
})
export class ListadoProgramas {
  private readonly api = inject(ProgramasApi);
  protected readonly programas = signal<Programa[]>([]);
  protected readonly universidades = signal<Universidad[]>([]);
  protected readonly termino = signal('');
  protected readonly paginaActual = signal(1);
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly formulario = signal(false);
  protected readonly importando = signal(false);
  protected readonly pendienteEliminar = signal<Programa | null>(null);
  protected readonly errorFormulario = signal('');
  protected edicion: Programa | null = null;
  protected nombre = '';
  protected universidadId: number | null = null;
  protected readonly filtrados = computed(() => {
    const termino = this.termino().trim().toLocaleLowerCase();
    return this.programas().filter((p) =>
      (p.nombre + ' ' + p.universidad).toLocaleLowerCase().includes(termino),
    );
  });
  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / 10)),
  );
  protected readonly desde = computed(() => (this.paginaActual() - 1) * 10);
  protected readonly programasPagina = computed(() =>
    this.filtrados().slice(this.desde(), this.desde() + 10),
  );
  constructor() {
    void this.cargar();
  }
  protected async cargar() {
    this.cargando.set(true);
    this.error.set('');
    try {
      const [programas, universidades] = await Promise.all([
        firstValueFrom(this.api.listar()),
        firstValueFrom(this.api.universidades()),
      ]);
      this.programas.set(programas);
      this.universidades.set(universidades);
      this.paginaActual.set(Math.min(this.paginaActual(), this.totalPaginas()));
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.cargando.set(false);
    }
  }
  protected buscar(valor: string) {
    this.termino.set(valor);
    this.paginaActual.set(1);
  }
  protected irAPagina(pagina: number) {
    if (pagina >= 1 && pagina <= this.totalPaginas()) this.paginaActual.set(pagina);
  }
  protected abrir(programa: Programa | null = null) {
    this.edicion = programa;
    this.nombre = programa?.nombre ?? '';
    this.universidadId = programa?.universidadId ?? null;
    this.errorFormulario.set('');
    this.formulario.set(true);
    this.aviso.set('');
  }
  protected async guardar() {
    if (this.guardando()) return;
    const nombre = this.nombre.trim();
    if (!nombre || nombre.length > 150 || !this.universidadId) {
      this.errorFormulario.set(
        'Escribe un nombre de hasta 150 caracteres y selecciona una universidad.',
      );
      return;
    }
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      const datos = { nombre, universidadId: this.universidadId };
      await firstValueFrom(
        this.edicion ? this.api.actualizar(this.edicion.id, datos) : this.api.crear(datos),
      );
      this.formulario.set(false);
      this.aviso.set(
        this.edicion ? 'Programa actualizado correctamente.' : 'Programa creado correctamente.',
      );
      await this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected async confirmarEliminar() {
    const programa = this.pendienteEliminar();
    if (!programa || this.guardando()) return;
    this.guardando.set(true);
    this.errorFormulario.set('');
    try {
      await firstValueFrom(this.api.desactivar(programa.id));
      this.pendienteEliminar.set(null);
      this.aviso.set(
        'Programa desactivado. Su información y las relaciones con residentes se conservan.',
      );
      await this.cargar();
    } catch (e) {
      this.errorFormulario.set(mensajeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
  protected solicitarEliminar(programa: Programa) {
    this.errorFormulario.set('');
    this.pendienteEliminar.set(programa);
  }
  protected cerrarImportacion() {
    this.importando.set(false);
    this.aviso.set('');
    void this.cargar();
  }
}
