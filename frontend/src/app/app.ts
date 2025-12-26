import { Component, ElementRef,ViewChild,AfterViewInit} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {NgOptimizedImage} from '@angular/common';
import {HostListener} from '@angular/core';
export interface canvasObject {
  id: string;
  type:'Q'|'M';
  x: number;
  y: number;
  width: number;
  height: number;
  productCount: number;
  color:string;
}
export interface MovingProduct {
  color:string;
  fromId:string;
  toId:string;
  progress:number;
}
export interface Connections {
  fromId:string;
  toId:string;
}
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NgOptimizedImage],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements AfterViewInit {
  movingProducts: MovingProduct[]=[];
  selectedObject:string |null = null;
   isConnecting:boolean = false;
   firstSelectedNode:canvasObject | null = null;
   mousePos = {x:0,y:0};
   connections:Connections[]=[];
   isRunning = false;
   isDragging = false;
   draggedObject:canvasObject|null = null;
   dragOffset = {x:0,y:0};
  private simulationIterval:any;
  public objects:canvasObject[]=[];
  private idCounter:number=0;
  private queueIcon = new Image();
  private machineIcon = new Image();
  public selectedTool: 'Q'|'M' | 'D' |null = null;
  @HostListener('window:resize')
  onResize() {
    const canvas = this.canvasRef.nativeElement;
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
    this.drawAll();
  }
  @HostListener('window:keydown',['$event'])
  handleKeyDown(event:KeyboardEvent) {
    if(event.key === 'Escape' || event.key === 'Tap'){
      event.preventDefault();
      this.isConnecting = false;
      this.firstSelectedNode = null;
      this.drawAll();
    }
    const key = event.key.toLowerCase();
    if(event.key === 'd'){
    this.selectedTool = 'D'
    }
    else if(event.key === 'm'){
    this.selectedTool = 'M'
    }
    else if(event.key === 'q'){
    this.selectedTool = 'Q'
    }
    else if(key === 'c'){
      if(confirm("clear the entire Canvas?")) this.Clear();
    }

  }
  @ViewChild("myCanvas") canvasRef!: ElementRef<HTMLCanvasElement>;
  private ctx!: CanvasRenderingContext2D;
  ngAfterViewInit() {
    this.initCanvas();
    this.queueIcon.src = 'assets/build-queue-svgrepo-com.svg';
    this.machineIcon.src = 'assets/machine-learning-solid-svgrepo-com.svg';
  }
   initCanvas() {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
  }
  selectTool(tool:'Q'|'M'|'D'){
    this.selectedTool = tool;
    const canvas = this.canvasRef.nativeElement;
    if (tool === 'D') canvas.style.cursor = 'no-drop'; // Or a custom trash cursor
    else if (tool) canvas.style.cursor = 'crosshair';
    else canvas.style.cursor = 'default';
  }


   drawAll(){
    if(!this.ctx) return;
    const canvas = this.canvasRef.nativeElement;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (this.isConnecting && this.firstSelectedNode) {
      this.ctx!.beginPath();
      this.ctx!.arc(this.firstSelectedNode.x, this.firstSelectedNode.y, 35, 0, Math.PI * 2);
      this.ctx!.fillStyle = 'rgba(255, 255, 255, 0.2)';
      this.ctx!.fill();
      this.ctx.setLineDash([5, 5]);
      this.ctx.strokeStyle = '#3498db';
      this.ctx.stroke();
      this.ctx.setLineDash([]); // Reset dash immediately!
    }
    this.connections.forEach(connection => {
      const from = this.objects.find(o => o.id === connection.fromId);
      const to = this.objects.find(o => o.id === connection.toId);
      if(from && to ){
        const dx = to.x - from.x;
        const dy = to.y - from.y;
        const angle = Math.atan2(dy, dx);
        const offset = 32;
        const edgeX = to.x - offset * Math.cos(angle);
        const edgeY = to.y - offset * Math.sin(angle);
        this.drawArrow(from.x,from.y,edgeX,edgeY,"white")
      };
    });
    if(this.isConnecting && this.firstSelectedNode){
      this.drawArrow(this.firstSelectedNode.x, this.firstSelectedNode.y, this.mousePos.x, this.mousePos.y, 'rgba(255,255,255,0.5)');
    }
    this.drawMovingProducts();
    this.objects.forEach(obj => {
      if(obj.type === 'Q'){
        this.drawQueue(obj);
      }else{
        this.drawMachine(obj);
      }
    });
  }

  private drawQueue(obj:canvasObject ) {
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
  private drawMachine(obj:canvasObject ) {
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
  drawMovingProducts(){
    this.movingProducts.forEach((prod) => {
      const from = this.objects.find(o => o.id === prod.fromId);
      const to = this.objects.find(o => o.id === prod.toId);
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
  Clear(){
    if(!this.ctx) return;
    const canvas = this.canvasRef.nativeElement;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);
    this.objects = [];
  }
  startSimulation(){
    this.isRunning = true;
    this.selectedTool = null;
    this.animateProducts();
    const eventSource= new EventSource('http://localhost:8080/api/simulation');
    eventSource.onmessage = (event) => {
      const updates = JSON.parse(event.data);
      updates.forEach((updates:any) => {
        const obj = this.objects.find(o => o.id === updates.id);
        if(obj){
          if(updates.isDispatching && updates.fromQueueId){
            this.movingProducts.push({
              color: updates.productColor, // The color of the product being moved
              fromId: updates.fromQueueId,
              toId: obj.id,
              progress: 0
            });
          }
          obj.color = updates.color;
          obj.productCount = updates.productCount;
        }
      });
    };
    eventSource.onerror = (event) => {
      console.error("error with sse: ",event);
      eventSource.close();
      this.isRunning = false;
    }
  }
  stopSimulation(){
    this.isRunning = false;
    clearInterval(this.simulationIterval);
    console.log("Simulation stopped");
  }
  replaySimulation(){
    this.stopSimulation();
    this.objects.forEach(obj => {
      if(obj.type === 'Q') obj.productCount = 0;
    });
    this.drawAll();
  }

  onCanvasClick(event:MouseEvent) {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const clickedObj = this.objects.find(obj =>
    x >= obj.x -30 && x<= obj.x + 30 && y>=obj.y-40 && y<= obj.y+40);
    if(clickedObj){
      if(!this.isConnecting){
        this.isConnecting = true;
        this.firstSelectedNode = clickedObj;
      }
      else{
        if(this.firstSelectedNode && this.firstSelectedNode.id === clickedObj.id){
          this.isConnecting = false;
          this.firstSelectedNode = null;
        }
        else if(this.firstSelectedNode && this.firstSelectedNode.type === clickedObj.type){
          this.isConnecting = false;
          this.firstSelectedNode = null;
        }
        else if(this.firstSelectedNode){
          const alreadyExists = this.connections.some(conn =>
          conn.fromId === this.firstSelectedNode!.id && conn.toId === clickedObj.id);
          const createsLoop = this.wouldCreateLoop(this.firstSelectedNode.id,clickedObj.id);
          if(!alreadyExists && !createsLoop){
            this.connections.push({
              fromId:this.firstSelectedNode.id,
              toId:clickedObj.id,
            });
          }
          this.isConnecting = false;
          this.firstSelectedNode = null;
        }
      }
      this.drawAll();
      return;
    }
    if(!clickedObj && this.isConnecting){
      this.isConnecting = false;
      this.firstSelectedNode = null;
      this.drawAll();
      return;
    }
    if(!this.isConnecting && (this.selectedTool === 'Q' || this.selectedTool === 'M')) {
      if(!this.selectedTool || !this.ctx) return;
      const rect = this.canvasRef.nativeElement.getBoundingClientRect();
      const x = Math.round((event.clientX - rect.left) /30)*30;
      const y = Math.round((event.clientY - rect.top) /30)*30;
      const newobj:canvasObject = {
        id:`${this.selectedTool}${this.idCounter++}`,
        type:this.selectedTool,
        x:x,
        y:y,
        width:40,
        height:60,
        productCount:0,
        color:'#95a5a6'
      };
      this.objects.push(newobj);
      this.drawAll();
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
    const hoverObj = this.objects.find(obj => x >= obj.x -30 && x <= obj.x +30 && y >= obj.y-40 && y<= obj.y+40);
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
    const clickedObj = this.objects.find(obj => x>=obj.x -30 && x<= obj.x + 30 && y >= obj.y -40 && y<= obj.y + 40 );
    if(this.selectedTool === 'D'){
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
        this.isDragging = true;
        this.draggedObject = clickedObj;
      }
      else if(this.selectedObject === clickedObj.id){
        this.isDragging = true;
        this.draggedObject = clickedObj;
      }
      else{
        this.isDragging = true;
        this.draggedObject = clickedObj;
      }
      this.drawAll();
      return;
    }
    if(this.isConnecting){
      this.isConnecting = false;
      this.firstSelectedNode = null;
    }
    else if(this.selectedTool === 'Q' || this.selectedTool === 'M'){
      const snappedX = Math.round(x/30) *30;
      const snappedY = Math.round(y/30) *30;
      const newObj:canvasObject = {
        id:`${this.selectedTool}${this.idCounter++}`,
        type:this.selectedTool,
        x:snappedX,
        y:snappedY,
        width:40,
        height:60,
        productCount:0,
        color:'#95a5a6'
      };
      this.objects.push(newObj);
    }
    this.drawAll();

  }

  onMouseUp(event:MouseEvent) {
    this.isDragging = false;
    this.draggedObject = null;
  }

  private drawArrow(fromX: number, fromY: number, toX: number, toY: number, color: string = 'black'){
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
  private wouldCreateLoop(startId:string, targetid:string):boolean{
    const visited = new Set<string>();
    const queue = [targetid];

    while (queue.length > 0) {
      const current = queue.shift()!;
      if (current === startId) return true; // Found a path back! Loop detected.

      if (!visited.has(current)) {
        visited.add(current);
        // Find all neighbors this node points to
        const neighbors = this.connections
          .filter(c => c.fromId === current)
          .map(c => c.toId);

        queue.push(...neighbors);
      }
    }
    return false; // No path back found, safe to connect.
  }
  private getProtAt(x:number, y:number, obj:canvasObject){
    const prots =[
      {x:obj.x, y:obj.y - 30},
      {x:obj.x, y:obj.y + 30},
      {x:obj.x -20, y:obj.y },
      {x:obj.x + 20, y:obj.y },
    ];
    return prots.find(p => Math.hypot(p.x -x , p.y - y) < 18);
  }
  private drawPorts(x:number, y:number){
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

  private finishConnection(clickedObj:canvasObject){
    if(!this.firstSelectedNode) return false;
    const sameNode = this.firstSelectedNode.id == clickedObj.id;
    const sametype = this.firstSelectedNode.type == clickedObj.type;
    const alreadyExists = this.connections.some(conn =>
    conn.fromId === this.firstSelectedNode!.id && conn.toId === clickedObj.id);
    const createsLoop =this.wouldCreateLoop(this.firstSelectedNode.id , clickedObj.id);
    if(!sameNode && !sametype && !alreadyExists && !createsLoop){
      this.connections.push({
        fromId:this.firstSelectedNode.id,
        toId:clickedObj.id,
      });
      this.isConnecting = false;
      this.firstSelectedNode = null;
      return true;
    }
    this.isConnecting = false;
    this.firstSelectedNode = null;
    return true;
  }

  Deleteobj(objId:string){
    this.objects = this.objects.filter(obj => obj.id !== objId);
    this.connections = this.connections.filter(conn => conn.fromId !== objId && conn.toId !== objId);
  }
  animateProducts(){
    if(!this.isRunning)return;
    this.movingProducts.forEach((prod,index) => {
      prod.progress += 0.01;
      if(prod.progress >= 1){
        const machine = this.objects.find(o => o.id === prod.toId);
        if(machine){
          machine.color = prod.color;
          setTimeout(() => {
            // 3. The product is now FINISHED
            machine.productCount++; // Increment the TOTAL history counter
            machine.color = '#95a5a6'; // Set back to Gray (Empty/Idle)

            console.log(`Machine ${machine.id} is now empty and ready for the next product.`);
            this.drawAll();
          }, 2000);
        }
        this.movingProducts.splice(index, 1);
      }
    });
    this.drawAll();
    requestAnimationFrame(() => this.animateProducts());
  }
  mockInterval:any;
  testSimulationWithMockData() {
    if (this.connections.length === 0) {
      alert("Please create at least one connection (Queue -> Machine) first!");
      return;
    }

    this.isRunning = true;
    this.animateProducts();

    this.mockInterval = setInterval(() => {
      // 1. Pick a random connection
      const randomConn = this.connections[Math.floor(Math.random() * this.connections.length)];
      const targetMachine = this.objects.find(o => o.id === randomConn.toId);

      // 2. BUSY CHECK: Only send product if machine is currently Gray (Idle)
      // and no product is currently traveling to it
      const isProductEnRoute = this.movingProducts.some(p => p.toId === randomConn.toId);

      if (targetMachine && (targetMachine.color === '#95a5a6' || !targetMachine.color) && !isProductEnRoute) {

        const colors = ['#e74c3c', '#f1c40f', '#9b59b6', '#3498db'];
        const randomColor = colors[Math.floor(Math.random() * colors.length)];

        // 3. Simulate "Product Dispatched"
        this.movingProducts.push({
          color: randomColor,
          fromId: randomConn.fromId,
          toId: randomConn.toId,
          progress: 0
        });

        // 4. Synchronization: Use the existing progress logic to update the machine
        // We don't need a separate setTimeout here anymore!
      }
    }, 500); // Check more frequently (every 0.5s) to find idle machines faster
  }
}
