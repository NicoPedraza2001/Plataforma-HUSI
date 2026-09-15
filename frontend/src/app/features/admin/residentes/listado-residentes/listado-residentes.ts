import { Component, computed, inject, signal } from '@angular/core';
import { ResidentesApi, Residente } from '../../../../services/residentes-api';
import { Consulta, Pagina, Parametros } from '../../../../services/consulta';
import { Catalogos, CatalogosApi } from '../../../../services/catalogos-api';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
@Component({selector: 'app-listado-residentes', imports: [Paginacion], templateUrl: './listado-residentes.html', styleUrl: './listado-residentes.css'})
export class ListadoResidentes {
 private readonly api = inject(ResidentesApi);
 private readonly catalogosApi = inject(CatalogosApi);
 protected readonly consulta = new Consulta<Pagina<Residente>>(params => this.api.listar(params));
 protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogosApi.listar());
 protected readonly termino = signal('');
 protected readonly paginaActual = computed(() => this.consulta.datos()?.pagina ?? this.paginaSolicitada());
 private readonly paginaSolicitada = signal(1);
 protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
 protected readonly desde = computed(() => (this.paginaActual() - 1) * 8);
 protected readonly residentesPagina = computed(() => this.consulta.datos()?.contenido ?? []);
 protected readonly cargando = computed(() => this.consulta.cargando() || this.catalogos.cargando());
 protected readonly error = computed(() => this.consulta.error() || this.catalogos.error());
 protected readonly universidadSeleccionada = signal('Todas');
 protected readonly programaSeleccionado = signal('Todos');
 protected readonly estadoSeleccionado = signal('Todos');
 protected readonly periodoSeleccionado = signal('Todos');
 protected readonly universidades = computed(() => ['Todas', ...(this.catalogos.datos()?.universidades ?? [])]);
 protected readonly programas = computed(() => ['Todos', ...(this.catalogos.datos()?.programas ?? [])]);
 protected readonly periodos = computed(() => ['Todos', ...(this.catalogos.datos()?.periodos ?? [])]);
 protected readonly estados = ['Todos', 'ACTIVO', 'INACTIVO'];
 protected claseBadge(estado: string) { return estado === 'ACTIVO' ? 'badge badge--activo' : 'badge badge--inactivo'; }
 constructor() { this.cargar(); this.catalogos.cargar(); }
 protected reintentar() { this.cargar(); this.catalogos.cargar(); }
 protected cargar(espera = 0) {
 const params: Parametros = { q: this.termino(), pagina: this.paginaSolicitada(), tamano: 8 };
 if(this.universidadSeleccionada() !== 'Todas') params['universidad'] = this.universidadSeleccionada();
 if(this.programaSeleccionado() !== 'Todos') params['programa'] = this.programaSeleccionado();
 if(this.estadoSeleccionado() !== 'Todos') params['estado'] = this.estadoSeleccionado();
 if(this.periodoSeleccionado() !== 'Todos') params['periodo'] = this.periodoSeleccionado();
 this.consulta.cargar(params, espera);
 }
 protected buscar(valor: string) { this.termino.set(valor); this.paginaSolicitada.set(1); this.cargar(300); }
 protected onUniversidadChange(valor: string) { this.universidadSeleccionada.set(valor); this.paginaSolicitada.set(1); this.cargar(); }
 protected onProgramaChange(valor: string) { this.programaSeleccionado.set(valor); this.paginaSolicitada.set(1); this.cargar(); }
 protected onEstadoChange(valor: string) { this.estadoSeleccionado.set(valor); this.paginaSolicitada.set(1); this.cargar(); }
 protected onPeriodoChange(valor: string) { this.periodoSeleccionado.set(valor); this.paginaSolicitada.set(1); this.cargar(); }
 protected irAPagina(pagina: number) { if(pagina<1 || pagina>this.totalPaginas() || this.cargando()) return; this.paginaSolicitada.set(pagina); this.cargar(); }
}
