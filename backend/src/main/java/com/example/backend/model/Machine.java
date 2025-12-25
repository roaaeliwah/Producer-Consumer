package com.example.backend.model;

import lombok.Data;

import java.awt.*;
import java.util.List;

@Data
public class Machine implements Runnable{
    private final int id;
    private MachineState state;
    private String currentColor = "GRAY";
    private List<SimQueue> inputQueues;
    private SimQueue outputQueue;
    private volatile boolean running = true;

    public Machine(int id, List<SimQueue> inputQueues, SimQueue outputQueue) {
        this.id = id;
        this.inputQueues = inputQueues;
        this.outputQueue = outputQueue;
        this.state = MachineState.IDLE;
    }

    @Override
    public void run() {
        try {
            while (running) {

                setState(MachineState.IDLE);

                Product product = dequeueFromAnyInput();

                // Start processing
                setState(MachineState.BUSY);
                setColor(product.getColor());

                process();

                // Finished processing
                setState(MachineState.FINISHED);
                flash();

                // Forward product
                if (outputQueue != null) {
                    outputQueue.put(product);
                }

                // Reset machine
                resetColor();
                setState(MachineState.IDLE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Product dequeueFromAnyInput() throws InterruptedException {
        while (running) {
            for (SimQueue queue : inputQueues) {
                if (queue.size() > 0) {
                    return queue.take();
                }
            }
            Thread.sleep(50); // prevent busy waiting
        }
        throw new InterruptedException("Machine stopped");
    }


    private void process() throws InterruptedException {
        int serviceTime = 500 + (int) (Math.random() * 1500);
        Thread.sleep(serviceTime);
    }

    private void flash() throws InterruptedException {
        Thread.sleep(200);
    }

    private void setState(MachineState newState) {
        this.state = newState;
        notifyObservers();
    }

    private void setColor(String color) {
        this.currentColor = color;
        notifyObservers();
    }

    private void resetColor() {
        this.currentColor = "GRAY";
        notifyObservers();
    }

    public void stopMachine() {
        running = false;
    }

    private void notifyObservers() {
        // Hook for UI observers / WebSocket updates / snapshot recording
    }
}
