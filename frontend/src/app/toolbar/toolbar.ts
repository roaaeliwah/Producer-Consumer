import { Component } from '@angular/core';
import {SimulationService} from '../Services/SimulationService';
@Component({
  selector: 'app-toolbar',
  standalone: true,
  imports: [],
  templateUrl: './toolbar.html',
  styleUrl: './toolbar.css',
})
export class Toolbar {
constructor(public simService: SimulationService) {}
get isRunning() {
  return this.simService.isRunning;
}
  selectTool(tool:'Q'|'M'|'D'){
    const newTool = this.simService.selectedTool === tool? null : tool;
    this.simService.setTool(newTool);
  }
  startSimulation(){
  this.simService.startSimulation();
  }
  stopSimulation(){
  this.simService.stopSimulation();
  }
  replaySimulation(){
  this.simService.replaySimulation();
  }
  Clear() {
    if(confirm("Are you sure you want to clear the entire floor?")) {
      this.simService.object$.next([]);
      this.simService.connections$.next([]);
      this.simService.movingProducts$.next([]);
    }
  }
}
