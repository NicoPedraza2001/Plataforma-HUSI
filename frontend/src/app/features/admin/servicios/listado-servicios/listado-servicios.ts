import { Component, computed, inject, signal } from '@angular/core';
import { ServicioService } from '../../../../services/servicio.service';
import { Servicio } from '../../../../models/servicio.model';
import { Consulta } from '../../../../shared/consulta/consulta';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { Catalogos } from '../../../../models/catalogos.model';
import { CatalogoService } from '../../../../services/catalogo.service';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
@Component({
  selector: 'app-listado-servicios',
  imports: [Paginacion],
  templateUrl: './listado-servicios.html',
  styleUrl: './listado-servicios.css',
})
export class ListadoServicios {
  private readonly api = inject(ServicioService);
  private readonly catalogosApi = inject(CatalogoService);
  protected readonly consulta = new Consulta<Pagina<Servicio>>((params) => this.api.listar(params));
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogosApi.listar());
  protected readonly termino = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly desde = computed(() => (this.paginaActual() - 1) * 10);
  protected readonly serviciosPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(
    () => this.consulta.cargando() || this.catalogos.cargando(),
  );
  protected readonly error = computed(() => this.consulta.error() || this.catalogos.error());
  protected readonly estado = signal('todos');
  constructor() {
    this.cargar();
    this.catalogos.cargar();
  }
  protected reintentar() {
    this.cargar();
    this.catalogos.cargar();
  }
  protected cargar(espera = 0) {
    const params: Parametros = { q: this.termino(), pagina: this.paginaSolicitada(), tamano: 10 };
    if (this.estado() !== 'todos')
      params['estado'] = this.estado() === 'activos' ? 'Activo' : 'Inactivo';
    this.consulta.cargar(params, espera);
  }
  protected buscar(valor: string) {
    this.termino.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }
  protected filtrarPorEstado(valor: string) {
    this.estado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }
  protected irAPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas() || this.cargando()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }
}
