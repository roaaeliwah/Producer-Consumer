import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Subject } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { CanvasObject, Connection, MovingProduct } from '../models/simulation';
import { ServerUpdates } from '../models/Updates';
@Injectable({
  providedIn: 'root'
})
export class SimulationService {
  private _isRunning$ = new BehaviorSubject<boolean>(false);
  public isRunning$ = this._isRunning$.asObservable();
  private clearRequested = new Subject<void>();
  public clearRequested$ = this.clearRequested.asObservable();
  public object$ = new BehaviorSubject<CanvasObject[]>([]);
  public connections$ = new BehaviorSubject<Connection[]>([]);
  public movingProducts$ = new BehaviorSubject<MovingProduct[]>([]);
  private animationFrameId: number | null = null;
  private eventSource: EventSource | null = null;
  private errorMessageSubject = new BehaviorSubject<string | null>(null);
  public errorMessage$ = this.errorMessageSubject.asObservable();
  private productCountSubject = new BehaviorSubject<number>(10);
  public productCount$ = this.productCountSubject.asObservable();
  private readonly defaultQueuePosition = { x: 150, y: 180 };
  private idCounter = 0;
  public isRunning = false;
  private _selectedTool$ = new BehaviorSubject<'Q' | 'M' | 'D' | null>(null);
  public selectedTool$ = this._selectedTool$.asObservable();

  setTool(tool: 'Q' | 'M' | 'D' | null) {
    this._selectedTool$.next(tool);
  }

  private readonly API_URL = 'http://localhost:8080/api';

  constructor(private readonly http: HttpClient) {
    this.resetToDefaultState();
  }
  get objects() { return this.object$.value; }
  get connections() { return this.connections$.value; }
  get movingProducts() { return this.movingProducts$.value; }
  get selectedTool() { return this._selectedTool$.value; }
  get _isRunning() {
    return this._isRunning$.value;
  }
  get productCount() {
    return this.productCountSubject.value;
  }
  addObject(type: 'Q' | 'M', x: number, y: number) {
    const newObj: CanvasObject = {
      id: `${type}${this.idCounter++}`,
      type,
      x: x,
      y: y,
      productCount: 0,
      color: '#95a5a6',
      state: 'IDLE',
      isFlashing: false,
    };
    this.object$.next([...this.objects, newObj]);
  }

  addConnection(fromId: string, toId: string) {
    if (this.wouldCreateLoop(fromId, toId)) return false;

    const newConn: Connection = { fromId, toId };
    this.connections$.next([...this.connections, newConn]);
    return true;
  }

  deleteObject(id: string) {
    if (id === 'Q0') {
      return;
    }

    const filteredObjs = this.objects.filter(o => o.id !== id);
    const filteredConnections = this.connections.filter(c => c.fromId !== id && c.toId !== id);
    this.object$.next(filteredObjs);
    this.connections$.next(filteredConnections);
  }

  startSimulation(productCount?: number) {
    if (this.isRunning) return;
    const products = productCount ?? this.productCount;
    const objectPayload = {
      queues: this.objects.filter(o => o.type === 'Q').map(q => q.id),
      machines: this.objects.filter(o => o.type === 'M').map(m => m.id)
    };
    const connectionPayload = this.connections.map(conn => {
      const source = this.objects.find(o => o.id === conn.fromId);
      const target = this.objects.find(o => o.id === conn.toId);

      if (source?.type === 'Q' && target?.type === 'M') {
        return { machineId: target.id, queueId: source.id, type: 'INPUT' };
      }
      if (source?.type === 'M' && target?.type === 'Q') {
        return { machineId: source.id, queueId: target.id, type: 'OUTPUT' };
      }
      return null;
    }).filter(c => c !== null);
    this.http.post(`${this.API_URL}/init/objects`, objectPayload).pipe(

      switchMap(() => {
        console.log("Objects created");
        return this.http.post(`${this.API_URL}/init/connections`, connectionPayload);
      }),

      switchMap(() => {
        console.log("Graph built");
        return this.http.post(`${this.API_URL}/simulation/start?productCount=${products}`, {});
      })

    ).subscribe({
      next: () => {
        console.log("Simulation running");
        this.isRunning = true;
        this._isRunning$.next(true);
        this.updateError(null);
        this.animate();
        this.connectToSse();
      },
      error: (err) => {
        console.error("Error:", err);
        this.isRunning = false;
        const message = this.extractErrorMessage(err, 'Failed to start simulation.');
        this.updateError(message);
        this._isRunning$.next(false);
      }
    });
  }

  private connectToSse() {
    // Create the connection to the streaming endpoint
    if (this.eventSource) {
      this.eventSource.close();
    }

    const eventSource = new EventSource(`${this.API_URL}/simulation/stream`);
    this.eventSource = eventSource;

    eventSource.onmessage = (event) => {
      try {
        // Parse the incoming JSON string into an array of ServerUpdates
        const rawData = JSON.parse(event.data);
        this.handleServerupdate(rawData);
      } catch (err) {
        console.error("Failed to parse SSE message:", err);
      }
    };

    eventSource.addEventListener('simulationStopped', () => {
      this.isRunning = false;
      this._isRunning$.next(false);
      this.updateError(null);
      if (this.animationFrameId) {
        cancelAnimationFrame(this.animationFrameId);
        this.animationFrameId = null;
      }
      eventSource.close();
      if (this.eventSource === eventSource) {
        this.eventSource = null;
      }
    });

    eventSource.onerror = (error) => {
      console.error("SSE Connection Error:", error);
      // If the connection drops, we stop the local animation
      this.isRunning = false;
      this._isRunning$.next(false);
      this.updateError('Connection to the simulation stream was lost.');
      eventSource.close();
      if (this.eventSource === eventSource) {
        this.eventSource = null;
      }
    };
  }

  private handleServerupdate(serverState: any) {
    const currentObjs = [...this.objects];

    if (serverState.machines) {
      serverState.machines.forEach((m: any) => {
        const obj = currentObjs.find(o => o.id === m.id);
        if (obj) {
          console.log(obj);
          // 1. TRIGGER: Animation from Queue to Machine
          if (obj.state === 'IDLE' && m.state === 'BUSY') {
            const sourceId = m.inputQueueIds?.[0];
            if (sourceId) {
              this.spawnProduct(m.currentColor, sourceId, m.id);
            }
          }

          // 2. TRIGGER: Animation from Machine to Queue (Output)
          if (obj.state === 'BUSY' && m.state === 'FINISHED') {
            const targetQueueId = m.outputQueueIds?.[0];
            if (targetQueueId) {
              this.spawnProduct(m.currentColor, m.id, targetQueueId);
            }
            this.triggerFlash(obj); // Flash when work is done
          }

          // 3. COLOR LOGIC: This is what fixes the "Snapshot" reset
          // Check if there is a product currently moving toward this machine
          const isProductIncoming = this.movingProducts.some(p => p.toId === m.id);

          if (m.state === 'IDLE' && !isProductIncoming) {
            // Only turn Gray if it's truly idle and no dot is about to hit it
            obj.color = '#95a5a6';
          } else if (m.state === 'BUSY' && m.currentColor?.startsWith('#')) {
            // If the machine is busy and we have a real color, we can update it
            // OR you can keep it as is and let the animate() hit handle it
            const productInFlight = this.movingProducts.find(p => p.toId === m.id);
            if (productInFlight) productInFlight.color = m.currentColor;
          }

          obj.state = m.state;
        }
      });
    }

    // Update Queues
    if (serverState.queues) {
      serverState.queues.forEach((q: any) => {
        const obj = currentObjs.find(o => o.id === q.id);
        if (obj) obj.productCount = q.size;
      });
    }

    this.object$.next(currentObjs);
  }

  private triggerFlash(obj: any) {
    obj.isFlashing = true;
    this.object$.next([...this.objects]); // Redraw ON

    setTimeout(() => {
      obj.isFlashing = false;
      this.object$.next([...this.objects]); // Redraw OFF
    }, 400);
  }
  private spawnProduct(color: string, fromId: string, toId: string) {
    const products = [...this.movingProducts, { color, fromId, toId, progress: 0 }];
    this.movingProducts$.next(products);
  }

  wouldCreateLoop(startId: string, targetId: string): boolean {
    const visited = new Set<string>();
    const queue = [targetId];
    while (queue.length > 0) {
      const current = queue.shift()!;
      if (current === startId) return true;
      if (!visited.has(current)) {
        visited.add(current);
        const neighbors = this.connections.filter(c => c.fromId === current).map(c => c.toId);
        queue.push(...neighbors);
      }
    }
    return false;
  }


  public animate() {
    if (!this.isRunning) return;

    const currentProducts = this.movingProducts.map(prod => ({
      ...prod,
      progress: prod.progress + 0.02
    }));

    currentProducts.forEach(prod => {
      if (prod.progress >= 1) {
        const targetMachine = this.objects.find(o => o.id === prod.toId);
        if (targetMachine) {
          // Apply the color the dot is carrying (which we updated above!)
          targetMachine.color = prod.color;
        }
      }
    });

    const remainingProducts = currentProducts.filter(prod => prod.progress < 1);
    this.movingProducts$.next(remainingProducts);

    // Refresh the UI to show machine color changes
    this.object$.next([...this.objects]);

    this.animationFrameId = requestAnimationFrame(() => { this.animate(); });
  }


  public stopSimulation() {
    this.http.post(`${this.API_URL}/simulation/stop`, {}).subscribe({
      next: (data: any) => {
        this.isRunning = false;
        if (this.animationFrameId) {
          cancelAnimationFrame(this.animationFrameId);
        }
        if (this.eventSource) {
          this.eventSource.close();
          this.eventSource = null;
        }
      },
      error: (err: any) => {
        console.log("failed to stop");
        const message = this.extractErrorMessage(err, 'Failed to stop simulation.');
        this.updateError(message);
      }
    })
    this._isRunning$.next(false);
  }
  public replaySimulation() {
    this.stopSimulation();

    const resetObjs = this.objects.map(obj => ({
      ...obj,
      productCount: 0,
      color: '#95a5a6',
      state: 'IDLE'
    }));
    this.object$.next(resetObjs);
    this.movingProducts$.next([]);
    this.http.post(`${this.API_URL}/simulation/replay`, {}).subscribe({
      next: () => {
        this.isRunning = true;
        this._isRunning$.next(true);
        this.updateError(null);
        this.animate();
        this.connectToSse();
      },
      error: (err) => {
        console.error("Failed to replay simulation", err);
        this.isRunning = false;
        const message = this.extractErrorMessage(err, 'Failed to replay simulation.');
        this.updateError(message);
        this._isRunning$.next(false);
      }
    });
  }

  public ClearAll() {
    this.resetLayout();
    this.http.post(`${this.API_URL}/simulation/reset`, {}).subscribe({})
    this.updateError(null);
    this.productCountSubject.next(10);
  }

  public adjustProductCount(delta: number) {
    const nextValue = Math.max(1, this.productCount + delta);
    this.productCountSubject.next(nextValue);
  }

  public resetLayout() {
    this.resetToDefaultState();
    this.clearRequested.next();
  }

  private resetToDefaultState() {
    const defaultQueue = this.buildDefaultQueue();
    this.object$.next([defaultQueue]);
    this.connections$.next([]);
    this.movingProducts$.next([]);
    this.idCounter = 1;
  }

  private buildDefaultQueue(): CanvasObject {
    return {
      id: 'Q0',
      type: 'Q',
      x: this.defaultQueuePosition.x,
      y: this.defaultQueuePosition.y,
      productCount: 0,
      color: '#95a5a6',
      state: 'IDLE',
      isFlashing: false,
    };
  }

  private updateError(message: string | null) {
    this.errorMessageSubject.next(message);
  }

  private extractErrorMessage(err: any, fallback: string): string {
    if (!err) {
      return fallback;
    }

    const candidate = err.error ?? err;

    if (typeof candidate === 'string') {
      try {
        const parsed = JSON.parse(candidate);
        return parsed?.message || parsed?.error || candidate;
      } catch {
        return candidate;
      }
    }

    if (candidate?.message) {
      return candidate.message;
    }

    if (candidate?.error) {
      return candidate.error;
    }

    if (err.message) {
      return err.message;
    }

    return fallback;
  }
}
