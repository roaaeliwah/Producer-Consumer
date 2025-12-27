import { Component } from '@angular/core';
import { SimulationService } from '../Services/SimulationService';
import { AsyncPipe, NgIf } from '@angular/common';
@Component({
  selector: 'app-toolbar',
  standalone: true,
  imports: [
    AsyncPipe,
    NgIf
  ],
  templateUrl: './toolbar.html',
  styleUrl: './toolbar.css',
})
export class Toolbar {
  constructor(public simService: SimulationService) { }

  selectTool(tool: 'Q' | 'M' | 'D') {
    const newTool = this.simService.selectedTool === tool ? null : tool;
    this.simService.setTool(newTool);
  }
  startSimulation() {
    this.simService.startSimulation();
  }
  stopSimulation() {
    this.simService.stopSimulation();
  }
  replaySimulation() {
    this.simService.replaySimulation();
  }
  incrementProducts() {
    this.simService.adjustProductCount(1);
  }
  decrementProducts() {
    this.simService.adjustProductCount(-1);
  }
  Clear() {
    if (confirm("Are you sure you want to clear the entire floor?")) {
      this.simService.ClearAll();
    }
  }
}
