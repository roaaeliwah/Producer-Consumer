package com.example.backend.model;

import com.example.backend.observer.QueueObserver;
import lombok.Data;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Data
public class Machine implements Runnable, QueueObserver {

    private Runnable onStateChange;
    private final String id;
    private volatile MachineState state = MachineState.IDLE;
    private volatile String currentColor = "GRAY";

    private List<SimQueue> inputQueues = new ArrayList<>();
    private List<SimQueue> outputQueues = new ArrayList<>();

    private final Object lock = new Object();

    private volatile boolean notified = false;
    private volatile boolean running = false;

    public Machine(String id) {
        this.id = id;
    }

    @Override
    public void update() {
        synchronized (lock) {
            notified = true;
            lock.notify(); // to wake up the thread
        }
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {

                setState(MachineState.IDLE);

                Product product = dequeueFromAnyInput();

                // Start processing
                setState(MachineState.BUSY);
                setColor(product.getColor());

                process();

                // Finished processing
                setState(MachineState.IDLE);
                flash();

                for (SimQueue queue : outputQueues) {
                    queue.put(product);
                }

                // Reset machine
                resetColor();
                setState(MachineState.IDLE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            setState(MachineState.IDLE);
            resetColor();
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
            for (SimQueue queue : inputQueues) {
                queue.attach(this);
            }
            synchronized (lock) {
                while (!notified && running) {
                    lock.wait(); // go to sleep until a queue update()
                }
                notified = false;
            }
            for (SimQueue queue : inputQueues) {
                queue.detach(this);
            }
        }
        throw new InterruptedException("Machine stopped");
    }

    private void process() throws InterruptedException {
        int serviceTime = ThreadLocalRandom.current().nextInt(1000, 3000);
        Thread.sleep(serviceTime);
    }

    private void flash() throws InterruptedException {
        Thread.sleep(200);
    }

    private void setState(MachineState newState) {
        this.state = newState;
        if (onStateChange != null)
            onStateChange.run();
    }

    private void setColor(String color) {
        this.currentColor = color;
        if (onStateChange != null)
            onStateChange.run();
    }

    private void resetColor() {
        this.currentColor = "GRAY";
        if (onStateChange != null)
            onStateChange.run();
    }

    public void stopMachine() {
        running = false;
    }

    public void reset() {
        stopMachine();
        resetColor();
        state = MachineState.IDLE;
    }
}
