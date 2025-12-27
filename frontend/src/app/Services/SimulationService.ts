import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CanvasObject, Connection, MovingProduct } from '../models/simulation';
import {ServerUpdates} from '../models/Updates';
@Injectable({
  providedIn: 'root'
})
export class SimulationService {
  public object$ = new BehaviorSubject<CanvasObject[]>([]);
  public connections$ = new BehaviorSubject<Connection[]>([]);
  public movingProducts$ = new BehaviorSubject<MovingProduct[]>([]);
  private animationFrameId:number | null = null;
  private idCounter = 0;
  public isRunning = false;
  private _selectedTool$ = new BehaviorSubject<'Q' | 'M' | 'D' | null>(null);
  public selectedTool$ = this._selectedTool$.asObservable();

  setTool(tool: 'Q' | 'M' | 'D' | null) {
    this._selectedTool$.next(tool);
  }

  constructor() {}
  get objects(){return this.object$.value;}
  get connections(){return this.connections$.value;}
  get movingProducts(){return this.movingProducts$.value;}
  get selectedTool() { return this._selectedTool$.value; }
  addObject(type:'Q'|'M',x:number, y:number){
    const newObj:CanvasObject = {
      id:`${type}${this.idCounter++}`,
      type,
      x:x,
      y:y,
      productCount:0,
      color:'#95a5a6'
    };
    this.object$.next([...this.objects, newObj]);
  }

  addConnection(fromId:string, toId:string){
    if(this.wouldCreateLoop(fromId,toId)) return false;

    const newConn:Connection = {fromId,toId};
    this.connections$.next([...this.connections, newConn]);
    return true;
  }

  deleteObject(id:string){

    const filteredObjs = this.objects.filter(o => o.id !== id);
    const filteredConnections = this.connections.filter(c => c.fromId !== id && c.toId !== id);
    this.object$.next(filteredObjs);
    this.connections$.next(filteredConnections);
  }

  startSimulation(){
    if(this.isRunning) return;
    this.isRunning = true;
    this.animate();
    const eventSource = new EventSource('http://localhost:8080/api/simulation');
    eventSource.onmessage = (event) => {
      const updates = JSON.parse(event.data);
      this.handleServerupdate(updates);
    };
    eventSource.onerror = (event) => {
      eventSource.close();
      this.isRunning = false;
    }
  }

  private handleServerupdate(updates:ServerUpdates[]){
    const currentObjs = [...this.objects];
    updates.forEach(update => {
      const obj = currentObjs.find(o => o.id === update.id);
      if(obj){
        obj.color = update.color;
        obj.productCount = update.productCount;
        if(update.isDispatching && update.fromQueueId){
          this.spawnProduct(update.productColor!, update.fromQueueId, obj.id);
        }
      }
    });
    this.object$.next(currentObjs);
  }

  private spawnProduct(color:string, fromId:string,toId:string){
    const products = [...this.movingProducts,{color,fromId,toId,progress:0}];
    this.movingProducts$.next(products);
  }

   wouldCreateLoop(startId:string, targetId:string):boolean{
    const visited = new Set<string>();
    const queue = [targetId];
    while(queue.length>0){
      const current = queue.shift()!;
      if(current === startId) return true;
      if(!visited.has(current)){
        visited.add(current);
        const neighbors = this.connections.filter(c => c.fromId === current).map(c => c.toId);
        queue.push(... neighbors);
      }
    }
    return false;
  }


  public animate(){
    if(!this.isRunning) return;
    const currentProducts = this.movingProducts.map(prod =>({
      ...prod,
      progress:prod.progress+0.02
    }));
    currentProducts.forEach(prod => {
      if(prod.progress >=1){
        const machine = this.objects.find(o => o.id === prod.toId);
        if(machine){
          machine.color = prod.color;
        }
      }
    });
    const remainingProducts = currentProducts.filter(prod => prod.progress < 1);
    this.movingProducts$.next(remainingProducts);
    this.animationFrameId= requestAnimationFrame(() => {this.animate()})
  }


  public stopSimulation(){
    this.isRunning = false;
    if(this.animationFrameId){
      cancelAnimationFrame(this.animationFrameId);
    }
  }
  public replaySimulation(){
    this.stopSimulation();

    const resetObjs = this.objects.map(obj =>({
      ...obj,
        productCount:0,
        color:'#95a5a6'
    }));
    this.object$.next(resetObjs);
    this.movingProducts$.next([]);
  }
}
