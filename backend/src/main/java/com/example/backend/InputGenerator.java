package com.example.backend;

import com.example.backend.model.Product;
import com.example.backend.model.SimQueue;

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
        for (int i = 0; i < productCount && running; i++) {
            try {
                Product p = new Product();   // generates ID + color
                outputQueue.put(p);   // notify machines via observer
                Thread.sleep(500 + (int)(Math.random() * 1500)); // random interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        running = false;
    }
}


