import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CanvasRenderer } from './canvas-renderer';

describe('CanvasRenderer', () => {
  let component: CanvasRenderer;
  let fixture: ComponentFixture<CanvasRenderer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CanvasRenderer]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CanvasRenderer);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
