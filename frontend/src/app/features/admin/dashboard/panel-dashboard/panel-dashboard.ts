import { Component, computed, inject, signal } from '@angular/core';
import { GraficoBarras } from '../componentes/grafico-barras/grafico-barras';
import { GraficoApilado } from '../componentes/grafico-apilado/grafico-apilado';
import { GraficoTendencia } from '../componentes/grafico-tendencia/grafico-tendencia';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { Consulta, Pagina, Parametros } from '../../../../services/consulta';
import { Catalogos, CatalogosApi } from '../../../../services/catalogos-api';
import { DashboardApi, DashboardDatos } from '../../../../services/dashboard-api';
import { RotacionesApi, Rotacion } from '../../../../services/rotaciones-api';
@Component({selector:'app-panel-dashboard',imports:[GraficoBarras,GraficoApilado,GraficoTendencia,Paginacion],templateUrl:'./panel-dashboard.html',styleUrl:'./panel-dashboard.css'})
export class PanelDashboard {
 private readonly api=inject(DashboardApi);
 private readonly rotacionesApi=inject(RotacionesApi);
 private readonly catalogosApi=inject(CatalogosApi);
 protected readonly consulta=new Consulta<DashboardDatos>(params=>this.api.consultar(params));
 protected readonly rotaciones=new Consulta<Pagina<Rotacion>>(params=>this.rotacionesApi.listar(params));
 protected readonly catalogos=new Consulta<Catalogos>(()=>this.catalogosApi.listar());
 protected readonly periodoElegido=signal('Todos');
 protected readonly universidadElegida=signal('Todas');
 protected readonly servicioElegido=signal('Todos');
 protected readonly corte=signal({periodo:'Todos',universidad:'Todas',servicio:'Todos'});
 protected readonly periodos=computed(()=>['Todos',...(this.catalogos.datos()?.periodos??[])]);
 protected readonly universidades=computed(()=>['Todas',...(this.catalogos.datos()?.universidades??[])]);
 protected readonly servicios=computed(()=>['Todos',...(this.catalogos.datos()?.servicios??[])]);
 protected readonly cargando=computed(()=>this.consulta.cargando()||this.rotaciones.cargando()||this.catalogos.cargando());
 protected readonly error=computed(()=>this.consulta.error()||this.rotaciones.error()||this.catalogos.error());
 protected readonly indicadores=computed(()=>this.consulta.datos()?.indicadores??[]);
 protected readonly rotacionesPorServicio=computed(()=>this.consulta.datos()?.rotacionesPorServicio??[]);
 protected readonly ocupacionPorServicio=computed(()=>this.consulta.datos()?.ocupacionPorServicio??[]);
 protected readonly distribucionUniversidad=computed(()=>this.consulta.datos()?.distribucionUniversidad??[]);
 protected readonly tendenciaInicios=computed(()=>this.consulta.datos()?.tendenciaInicios??[]);
 protected readonly alertas=computed(()=>this.consulta.datos()?.alertas??[]);
 protected readonly totalInicios=computed(()=>this.tendenciaInicios().reduce((total,punto)=>total+punto.valor,0));
 protected readonly rotacionesProximas=computed(()=>this.rotaciones.datos()?.contenido??[]);
 protected readonly paginaActual=computed(()=>this.rotaciones.datos()?.pagina??1);
 protected readonly totalPaginas=computed(()=>this.rotaciones.datos()?.totalPaginas??1);
 constructor() { this.catalogos.cargar(); this.actualizar(); }
 protected elegirPeriodo(valor:string) { this.periodoElegido.set(valor); }
 protected elegirUniversidad(valor:string) { this.universidadElegida.set(valor); }
 protected elegirServicio(valor:string) { this.servicioElegido.set(valor); }
 protected actualizar() {
  this.corte.set({periodo:this.periodoElegido(),universidad:this.universidadElegida(),servicio:this.servicioElegido()});
  this.consulta.cargar(this.parametros()); this.irAPagina(1);
 }
 protected reintentar() { this.catalogos.cargar(); this.actualizar(); }
 private parametros():Parametros {
  const p:Parametros={}; const corte=this.corte();
  if(corte.periodo!=='Todos') p['periodo']=corte.periodo;
  if(corte.universidad!=='Todas') p['universidad']=corte.universidad;
  if(corte.servicio!=='Todos') p['servicio']=corte.servicio;
  return p;
 }
 protected irAPagina(pagina:number) { this.rotaciones.cargar({...this.parametros(),pagina,tamano:10,proximas:true}); }
 protected etiquetaEstado(estado:string) { return estado; }
}
