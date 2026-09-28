import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilaCargaInicial } from '../../../../models/carga-inicial.model';
import { CAMPOS_CARGA } from '../importar-rotaciones/campos-carga';
@Component({
  selector: 'app-revisar-carga',
  imports: [FormsModule],
  templateUrl: './revisar-carga.html',
  styleUrl: './revisar-carga.css',
})
export class RevisarCarga {
  readonly filas = input.required<FilaCargaInicial[]>();
  readonly bloqueado = input(false);
  readonly cambiar = output<FilaCargaInicial[]>();
  readonly edicion = output<boolean>();
  protected readonly campos = CAMPOS_CARGA;
  protected readonly pagina = signal(1);
  protected readonly consulta = signal('');
  protected readonly editando = signal<FilaCargaInicial | null>(null);
  protected readonly filtradas = computed(() => {
    const q = this.consulta().trim().toLowerCase();
    return this.filas().filter((f) =>
      [f.fila, f.residente, f.documento, f.servicio, f.asignatura].some((v) =>
        String(v).toLowerCase().includes(q),
      ),
    );
  });
  protected readonly paginas = computed(() => Math.max(1, Math.ceil(this.filtradas().length / 20)));
  protected readonly actual = computed(() => Math.min(this.pagina(), this.paginas()));
  protected readonly visibles = computed(() =>
    this.filtradas().slice((this.actual() - 1) * 20, this.actual() * 20),
  );
  protected mover(fila: FilaCargaInicial, direccion: number) {
    if (this.bloqueado()) return;
    const todas = [...this.filas()],
      i = todas.findIndex((f) => f.fila === fila.fila),
      j = i + direccion;
    if (j < 0 || j >= todas.length) return;
    [todas[i], todas[j]] = [todas[j], todas[i]];
    this.cambiar.emit(todas);
  }
  protected editar(f: FilaCargaInicial) {
    if (!this.bloqueado()) {
      this.editando.set({ ...f });
      this.edicion.emit(true);
    }
  }
  protected cancelar() {
    this.editando.set(null);
    this.edicion.emit(false);
  }
  protected guardar() {
    const fila = this.editando();
    if (!fila || this.bloqueado()) return;
    this.cambiar.emit(this.filas().map((f) => (f.fila === fila.fila ? { ...fila } : f)));
    this.cancelar();
  }
}
