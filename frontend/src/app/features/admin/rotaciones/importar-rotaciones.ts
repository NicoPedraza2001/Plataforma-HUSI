import { Component,computed,inject,output,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { RotacionesApi,FilaRotacion,ResultadoRotaciones } from '../../../services/rotaciones-api';
import { mensajeError } from '../../../services/api-error';
import { leerExcel,descargarLibro,HojaExcel } from '../../../shared/excel/archivo-excel';
@Component({selector:'app-importar-rotaciones',imports:[FormsModule],templateUrl:'./importar-rotaciones.html',styleUrl:'./importar-rotaciones.css'})
export class ImportarRotaciones {
 readonly cerrar=output<void>(); private readonly api=inject(RotacionesApi);
 protected readonly ocupado=signal(false);protected readonly error=signal('');protected readonly hojas=signal<HojaExcel[]>([]);
 protected readonly archivo=signal('');protected readonly resultado=signal<ResultadoRotaciones|null>(null);protected readonly terminado=signal(false);
 protected hoja=0;protected encabezado=1;protected columnas=[-1,-1,-1,-1];
 protected readonly campos=['Asignatura','Servicio','Fecha de inicio','Fecha de fin'];
 private validadas:FilaRotacion[]=[];
 protected readonly detallePagina=signal(1);
 protected readonly detalle=computed(()=>this.resultado()?.filas.slice((this.detallePagina()-1)*20,this.detallePagina()*20)??[]);
 protected readonly paginas=computed(()=>Math.max(1,Math.ceil((this.resultado()?.total??0)/20)));
 protected encabezados(){return this.hojas()[this.hoja]?.filas[this.encabezado-1]??[];}
 protected reiniciar(){if(this.ocupado())return;this.hojas.set([]);this.archivo.set('');this.resultado.set(null);this.terminado.set(false);this.validadas=[];this.error.set('');this.columnas=[-1,-1,-1,-1];}
 protected invalidar(){this.resultado.set(null);this.validadas=[];this.detallePagina.set(1);}
 protected cambiarHoja(){this.encabezado=1;this.columnas=[-1,-1,-1,-1];this.invalidar();}
 protected async seleccionar(event:Event){const input=event.target as HTMLInputElement;if(input.files?.[0])await this.cargar(input.files[0]);input.value='';}
 protected async soltar(e:DragEvent){e.preventDefault();if(e.dataTransfer?.files[0])await this.cargar(e.dataTransfer.files[0]);}
 protected async cargar(file:File){if(this.ocupado())return;this.reiniciar();this.ocupado.set(true);try{this.hojas.set(await leerExcel(file));this.archivo.set(file.name);this.hoja=0;this.encabezado=1;const h=this.encabezados().map(x=>x.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim());this.columnas=[h.indexOf('asignatura'),h.indexOf('servicio'),h.findIndex(x=>['inicio','fecha de inicio','fecha inicio'].includes(x)),h.findIndex(x=>['fin','fecha de fin','fecha fin'].includes(x))];}catch(e){this.error.set(e instanceof Error?e.message:'No se pudo leer el Excel.');}finally{this.ocupado.set(false);}}
 protected async validar(){
  if(this.ocupado())return;this.error.set('');this.invalidar();
  const filas=this.hojas()[this.hoja]?.filas??[];
  if(!Number.isInteger(this.encabezado)||this.encabezado<1||this.encabezado>filas.length||new Set(this.columnas).size!==4||this.columnas.some(c=>c<0||c>=this.encabezados().length)){this.error.set('Selecciona la fila de encabezados y cuatro columnas diferentes.');return;}
  const datos=filas.slice(this.encabezado).map((f,i)=>({fila:this.encabezado+i+1,asignatura:(f[this.columnas[0]]??'').trim(),servicio:(f[this.columnas[1]]??'').trim(),inicio:(f[this.columnas[2]]??'').trim(),fin:(f[this.columnas[3]]??'').trim()})).filter(f=>f.asignatura||f.servicio||f.inicio||f.fin);
  if(!datos.length||datos.length>1000){this.error.set('Elige entre 1 y 1000 filas. Las filas vacías se ignoran.');return;}
  this.ocupado.set(true);try{this.resultado.set(await firstValueFrom(this.api.validar(datos)));this.validadas=datos;}catch(e){this.error.set(mensajeError(e));}finally{this.ocupado.set(false);}
 }
 protected async importar(){if(this.ocupado()||!this.resultado()?.VALIDO||this.terminado())return;this.ocupado.set(true);this.error.set('');try{this.resultado.set(await firstValueFrom(this.api.importar(this.validadas)));this.terminado.set(true);this.detallePagina.set(1);}catch(e){this.error.set(mensajeError(e)+' Si se perdió la conexión, vuelve a validar: las rotaciones ya guardadas se omitirán.');this.invalidar();}finally{this.ocupado.set(false);}}
 protected async descargar(errores=false){try{if(errores)await descargarLibro('resultado-rotaciones.xlsx',['Fila','Estado','Mensaje'],(this.resultado()?.filas??[]).map(f=>[f.fila,f.estado,f.mensaje]),'Resultado');else await descargarLibro('plantilla-rotaciones.xlsx',this.campos,[],'Rotaciones');}catch{this.error.set('No se pudo descargar el archivo.');}}
}
