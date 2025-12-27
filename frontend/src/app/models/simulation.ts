export interface CanvasObject {
  id: string;
  type: 'Q' | 'M';
  x: number;
  y: number;
  productCount: number;
  color: string;
  state: string;
  isFlashing: boolean;
}

export interface Connection {
  fromId: string;
  toId: string;
}

export interface MovingProduct {
  color: string;
  fromId: string;
  toId: string;
  progress: number;
}
