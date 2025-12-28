import { Component, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toolbar } from './toolbar/toolbar';
import { SimulationService } from './Services/SimulationService';
import { CanvasRenderer } from './canvas-renderer/canvas-renderer';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Toolbar, CanvasRenderer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor(private simService: SimulationService) { }
  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape' || event.key === 'Tap') {
      this.simService.setTool(null);
      /*this.canvasRenderer.isConnecting = false;*/
    }
    const key = event.key.toLowerCase();
    if (event.key === 'd') {
      this.simService.setTool('D');
    }
    else if (event.key === 'm') {
      this.simService.setTool('M');
    }
    else if (event.key === 'q') {
      this.simService.setTool("Q")
    }
    else if (key === 'c') {
      if (confirm("clear the entire Canvas?")) {
        this.simService.resetLayout();
      }
    }

  }

}
