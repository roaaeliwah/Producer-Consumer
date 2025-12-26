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
   isConnecting:boolean = false;
   firstSelectedNode:canvasObject | null = null;
   mousePos = {x:0,y:0};
   connections:Connections[]=[];
   isRunning = false;
  private simulationIterval:any;
  public objects:canvasObject[]=[];
  private idCounter:number=0;
  private queueIcon = new Image();
  private machineIcon = new Image();
  public selectedTool: 'Q'|'M' | null = null;
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
  }
  @ViewChild("myCanvas") canvasRef!: ElementRef<HTMLCanvasElement>;
  private ctx!: CanvasRenderingContext2D;
  ngAfterViewInit() {
    this.initCanvas();
    this.queueIcon.src = 'assets/build-queue-svgrepo-com.svg';
    this.machineIcon.src = 'assets/machine-learning-solid-svgrepo-com.svg';
  }
  private initCanvas() {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
  }
  selectTool(tool:'Q'|'M'){
    this.selectedTool = tool;
  }


  public drawAll(){
    if(!this.ctx) return;
    const canvas = this.canvasRef.nativeElement;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (this.isConnecting && this.firstSelectedNode) {
      this.ctx!.beginPath();
      this.ctx!.arc(this.firstSelectedNode.x, this.firstSelectedNode.y, 35, 0, Math.PI * 2);
      this.ctx!.fillStyle = 'rgba(255, 255, 255, 0.2)';
      this.ctx!.fill();
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
  }
  private drawMachine(obj:canvasObject ) {
    const ctx = this.ctx!;
    const {x,y} = obj;
    ctx.beginPath();
    ctx.arc(x, y, 30, 0, Math.PI*2);
    ctx.fillStyle = '#2ecc71';
    ctx.fill();
    ctx.strokeStyle = 'white';
    ctx.lineWidth = 3;
    ctx.stroke();
    ctx.drawImage(this.machineIcon, x-15, y-15, 30, 30);
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
    console.log("Starting Simulation");
    this.simulationIterval = setInterval(()=>{
      this.testSimUpdate();
    }, 1000);
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
  private testSimUpdate(){
    this.objects.forEach(obj => {
      if(obj.type === 'Q'){
        obj.productCount += Math.floor(Math.random() * 3);
      }
    });
    this.drawAll();
  }

  onCanvasClick(event:MouseEvent) {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const clickedObj = this.objects.find(obj =>
    x >= obj.x -20 && x<= obj.x + 20 && y>=obj.y-30 && y<= obj.y+30);
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
    if(!this.isConnecting && this.selectedTool){
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
        productCount:0
      };
      this.objects.push(newobj);
      this.drawAll();
    }
  }
  onMouseMove(event:MouseEvent) {
    if(this.isConnecting){
      const rect = this.canvasRef.nativeElement.getBoundingClientRect();
      this.mousePos.x = event.clientX - rect.left;
      this.mousePos.y = event.clientY - rect.top;
      this.drawAll();
    }
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
    const outgoind = this.connections.filter(c=> c.fromId === targetid);
    for(let conn of outgoind){
      if(conn.toId === startId){
        return true;
      }
      if(this.wouldCreateLoop(startId,conn.toId)){
        return true;
      }
    }
    return false;
  }
}
