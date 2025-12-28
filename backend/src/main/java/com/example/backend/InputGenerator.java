package com.example.backend;

import com.example.backend.model.Product;
import com.example.backend.model.SimQueue;

import java.util.concurrent.ThreadLocalRandom;

public class InputGenerator implements Runnable {

    private final SimQueue outputQueue;
    private final int productCount;
    private volatile boolean running = true;

    public InputGenerator(SimQueue outputQueue, int productCount) {
        this.outputQueue = outputQueue;
        this.productCount = productCount;
    }

    @Override
    public void run() {


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (int i = 0; i < productCount && running; i++) {
            try {
                Product p = new Product();   // generates ID + color
                outputQueue.put(p);   // notify machines via observer
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000)); // random interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        running = false;
    }
}


