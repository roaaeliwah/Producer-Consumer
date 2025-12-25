package com.example.backend.model;

import com.example.backend.observer.QueueObserver;
import lombok.Data;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Data
public class Machine implements Runnable, QueueObserver {
    private final String id;
    private MachineState state;
    private String currentColor = "GRAY";

    private List<SimQueue> inputQueues = new ArrayList<>();
    private SimQueue outputQueue;

    private final Object lock = new Object();

    private volatile boolean notified = false;
    private volatile boolean running = true;

    public Machine(String id) {
        this.id = id;
        this.state = MachineState.IDLE;
    }
    @Override
    public void update(){
        synchronized (lock){
            notified = true;
            lock.notify(); // to wake up the thread
        }
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
                Product product = queue.take();
                // take return null if empty
                if (product != null) {
                    return product;
                }
            }
            // if all the queues are empty register as ready
            for(SimQueue queue : inputQueues){
                queue.attach(this);
            }
            synchronized (lock){
                while (!notified && running){
                    lock.wait(); // go to sleep until a queue update()
                }
                notified = false;
            }
            for(SimQueue queue : inputQueues){
                queue.detach(this);
            }
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
