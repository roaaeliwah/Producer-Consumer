import {Component, HostListener,AfterViewInit,ViewChild,ElementRef} from '@angular/core';
import {SimulationService} from '../Services/SimulationService';
import {CanvasObject} from '../models/simulation';

@Component({
  selector: 'app-canvas-renderer',
  standalone: true,
  imports: [],
  templateUrl: './canvas-renderer.html',
  styleUrl: './canvas-renderer.css',
})
export class CanvasRenderer implements AfterViewInit {
@ViewChild('myCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;
private ctx!: CanvasRenderingContext2D;
selectedObject:string |null = null;
  private queueIcon = new Image();
  private machineIcon = new Image();
  isDragging = false;
  draggedObject: any = null;
  isConnecting = false;
  firstSelectedNode: any = null;
  mousePos = { x: 0, y: 0 };
constructor(public simService:SimulationService) {}
  @HostListener('window:resize')
  onResize() {
    this.resizeCanvas();
    this.drawAll();
  }
  ngAfterViewInit(): void {
  this.initCanvas();
  this.loadAssets();
  this.simService.object$.subscribe(() => this.drawAll());
  this.simService.connections$.subscribe(() => this.drawAll());
  this.simService.movingProducts$.subscribe(() => this.drawAll());
  this.simService.selectedTool$.subscribe(tool => {
    const canvas = this.canvasRef.nativeElement;
    if (tool === 'D') canvas.style.cursor = 'no-drop';
    else if (tool) canvas.style.cursor = 'crosshair';
    else canvas.style.cursor = 'default';
  });

  }
  initCanvas() {
  this.ctx = this.canvasRef.nativeElement.getContext('2d')!;
  this.resizeCanvas();
  }
  loadAssets() {
    this.queueIcon.src = 'assets/build-queue-svgrepo-com.svg';
    this.machineIcon.src = 'assets/machine-learning-solid-svgrepo-com.svg';
    // Redraw once images are ready
    this.queueIcon.onload = () => this.drawAll();
    this.machineIcon.onload = () => this.drawAll();
  }
  resizeCanvas(){
    const canvas = this.canvasRef.nativeElement;
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
  }


  drawAll() {
  if(!this.ctx || !this.canvasRef) return;
  this.clearPixels();
  this.simService.connections.forEach(conn => {
    const from = this.simService.objects.find(o => o.id === conn.fromId);
    const to = this.simService.objects.find(o => o.id === conn.toId);
    if(from && to) {
      const dx = to.x - from.x;
      const dy = to.y - from.y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      // Define how much space to leave at the end (the radius of the object)
      const offset = (to.type === 'M') ? 32 : 25; // Machines are rounder, Queues are squares

      // Calculate the point on the edge
      const targetX = to.x - (dx / distance) * offset;
      const targetY = to.y - (dy / distance) * offset;
      this.drawArrow(from.x,from.y,targetX,targetY);
    }
  });
  this.drawMovingProducts();
    this.simService.objects.forEach(obj => {
      if (obj.type === 'Q') this.drawQueue(obj);
      else this.drawMachine(obj);
    });
    if(this.isConnecting && this.firstSelectedNode) {
      this.ctx!.setLineDash([5, 5]); // Make the preview line dashed
      this.drawArrow(
        this.firstSelectedNode.x,
        this.firstSelectedNode.y,
        this.mousePos.x,
        this.mousePos.y,
        '#3498db' // Blue color for preview
      );
      this.ctx!.setLineDash([]); // Reset to solid line
    }
  }

  onMouseMove(event:MouseEvent) {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    if(this.isDragging && this.draggedObject){
      this.draggedObject.x = Math.round(x/30) *30;
      this.draggedObject.y = Math.round(y/30) *30;
      this.drawAll();
    }
    if(this.isConnecting){
      this.mousePos.x = event.clientX - rect.left;
      this.mousePos.y = event.clientY - rect.top;
      this.drawAll();
    }
    const hoverObj = this.simService.objects.find(obj => x >= obj.x -30 && x <= obj.x +30 && y >= obj.y-40 && y<= obj.y+40);
    if(hoverObj){
      const overPort = this.getProtAt(x,y,hoverObj);
      this.canvasRef.nativeElement.style.cursor = overPort? 'pointer' : 'move';
    }
    else{
      this.canvasRef.nativeElement.style.cursor = 'default';
    }
  }

  onMouseDown(event:MouseEvent){
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const clickedObj = this.simService.objects.find(obj => x>=obj.x -30 && x<= obj.x + 30 && y >= obj.y -40 && y<= obj.y + 40 );
    if(this.simService.selectedTool === 'D'){
      if(clickedObj){
        this.Deleteobj(clickedObj.id);
        this.drawAll();
      }
      return;
    }
    if(this.isConnecting && clickedObj){
      if(this.finishConnection(clickedObj)){
        this.drawAll();
        return;
      }
    }

    if(clickedObj){
      const port = this.getProtAt(x,y,clickedObj);
      if(port) {
        this.isConnecting = true;
        this.firstSelectedNode = clickedObj;
        this.selectedObject = clickedObj.id;
      }
      else{
        this.isDragging = true;
        this.draggedObject = clickedObj;
        this.selectedObject = clickedObj.id;
      }
      this.drawAll();
      return;
    }

    if(this.isConnecting){
      this.isConnecting = false;
      this.firstSelectedNode = null;
    }
    else if(this.simService.selectedTool === 'Q' || this.simService.selectedTool === 'M'){
      const snappedX = Math.round(x/30) *30;
      const snappedY = Math.round(y/30) *30;
      this.simService.addObject(this.simService.selectedTool,snappedX,snappedY);
    }
    this.selectedObject = null;
    this.drawAll();

  }

  onMouseUp(event:MouseEvent) {
    this.isDragging = false;
    this.draggedObject = null;
  }

  drawQueue(obj:CanvasObject ) {
    const ctx = this.ctx!;
    const{x,y,productCount} = obj;
    const w = 40;
    const h = 45;
    const topLeftX = x- w/2;
    const topLeftY = y- h/2;
    ctx.fillStyle = '#f9f9f9';
    ctx.fillRect(topLeftX, topLeftY, w, h);
    ctx.strokeStyle='#333';
    ctx.lineWidth = 2;
    ctx.strokeRect(topLeftX, topLeftY, w, h);
    ctx.drawImage(this.queueIcon, x-15, y-20, 30, 30);
    const badgeX = x+20;
    const badgeY = y-22;

    this.ctx!.beginPath();
    this.ctx!.arc(badgeX, badgeY, 10,0,Math.PI*2);
    this.ctx!.fillStyle = "red";
    this.ctx!.fill();
    this.ctx!.strokeStyle="white";
    this.ctx!.stroke();

    // for the text
    this.ctx!.fillStyle = "white";
    this.ctx!.font ="bold 10px Arial";
    this.ctx!.textAlign="center";
    this.ctx!.textBaseline="middle";
    this.ctx!.fillText(productCount.toString(),badgeX,badgeY);
    this.drawPorts(obj.x,obj.y);
  }
  drawMachine(obj:CanvasObject ) {
    const ctx = this.ctx!;
    const {x,y} = obj;
    ctx.save();
    ctx.beginPath();
    ctx.arc(x, y, 30, 0, Math.PI*2);
    ctx.fillStyle = obj.color || '#95a5a6';
    ctx.fill();
    ctx.strokeStyle = 'white';
    ctx.lineWidth = 3;
    ctx.stroke();
    if(this.machineIcon.complete){
      ctx.drawImage(this.machineIcon, x-15, y-15, 30, 30);
    }
    ctx.restore();
  }
  drawArrow(fromX: number, fromY: number, toX: number, toY: number, color: string = 'white'){
    const headLength =12;
    const dx = toX-fromX;
    const dy = toY-fromY;
    const angle = Math.atan2(dy, dx);
    this.ctx!.strokeStyle = color;
    this.ctx!.lineWidth = 2;
    this.ctx!.lineCap = 'round';

    this.ctx!.beginPath();
    this.ctx!.moveTo(fromX, fromY);
    this.ctx!.lineTo(toX, toY);
    this.ctx!.stroke();

    this.ctx!.beginPath();
    this.ctx!.moveTo(toX, toY);
    this.ctx!.lineTo(toX - headLength * Math.cos(angle - Math.PI / 7), toY - headLength * Math.sin(angle - Math.PI / 7));
    this.ctx!.moveTo(toX, toY);
    this.ctx!.lineTo(toX - headLength * Math.cos(angle + Math.PI / 7), toY - headLength * Math.sin(angle + Math.PI / 7));

    this.ctx!.stroke();

  }
  drawMovingProducts(){
    this.simService.movingProducts.forEach((prod) => {
      const from = this.simService.objects.find(o => o.id === prod.fromId);
      const to = this.simService.objects.find(o => o.id === prod.toId);
      if(from && to ){
        const currentX = from.x + (to.x-from.x)* prod.progress;
        const currentY = from.y + (to.y-from.y)* prod.progress;
        this.ctx!.beginPath();
        this.ctx!.arc(currentX, currentY, 8, 0, Math.PI*2);
        this.ctx!.fillStyle = prod.color;
        this.ctx!.fill();
        this.ctx!.strokeStyle = 'white';
        this.ctx!.lineWidth = 2;
        this.ctx!.stroke();
      }
    });
  }

  drawPorts(x:number, y:number){
    const ports = [
      { x: x, y: y - 30 }, { x: x, y: y + 30 },
      { x: x - 20, y: y }, { x: x + 20, y: y }
    ];
    this.ctx!.fillStyle = "#3498db"
    ports.forEach(p => {
      this.ctx!.beginPath();
      this.ctx!.arc(p.x,p.y,4,0,2*Math.PI);
      this.ctx!.fill();
    });
  }
  Deleteobj(objId:string){
  this.simService.deleteObject(objId);
}
   getProtAt(x:number, y:number, obj:CanvasObject){
    const prots =[
      {x:obj.x, y:obj.y - 30},
      {x:obj.x, y:obj.y + 30},
      {x:obj.x -20, y:obj.y },
      {x:obj.x + 20, y:obj.y },
    ];
    return prots.find(p => Math.hypot(p.x -x , p.y - y) < 18);
  }

   finishConnection(clickedObj:CanvasObject){
    if(!this.firstSelectedNode) return false;
    const sameNode = this.firstSelectedNode.id == clickedObj.id;
    const sametype = this.firstSelectedNode.type == clickedObj.type;
    const alreadyExists = this.simService.connections.some(conn =>
      conn.fromId === this.firstSelectedNode!.id && conn.toId === clickedObj.id);
    const createsLoop =this.simService.wouldCreateLoop(this.firstSelectedNode.id , clickedObj.id);
    if(!sameNode && !sametype && !alreadyExists && !createsLoop){
      this.simService.addConnection(this.firstSelectedNode.id, clickedObj.id);
      this.isConnecting = false;
      this.firstSelectedNode = null;
      return true;
    }
    this.isConnecting = false;
    this.firstSelectedNode = null;
    return true;
  }
  Clear(){
    if(!this.ctx) return;
    const canvas = this.canvasRef.nativeElement;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);
    this.simService.object$.next([]);
    this.simService.connections$.next([]);
    this.simService.movingProducts$.next([]);

  }
  private clearPixels() {
    if (!this.ctx) return;
    const canvas = this.canvasRef.nativeElement;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
}
