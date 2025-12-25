package com.example.backend;

import com.example.backend.model.Product;
import com.example.backend.model.SimQueue;
import com.example.backend.util.ColorGenerator;

public class InputGenerator implements Runnable {

    private final SimQueue outputQueue;
    private volatile boolean running = true;
    private int productCounter = 0;
    private ColorGenerator colorGenerator;

    // Arrival time bounds (milliseconds)
    private final int minArrivalTime;
    private final int maxArrivalTime;

    public InputGenerator(SimQueue outputQueue,
                          int minArrivalTime,
                          int maxArrivalTime) {
        this.outputQueue = outputQueue;
        this.minArrivalTime = minArrivalTime;
        this.maxArrivalTime = maxArrivalTime;
    }

    @Override
    public void run() {
        try {
            while (running) {

                // 1. Wait for next arrival
                int arrivalTime = minArrivalTime +
                        (int) (Math.random() * (maxArrivalTime - minArrivalTime));
                Thread.sleep(arrivalTime);

                // 2. Generate a new product
                Product product = new Product(productCounter++, colorGenerator.randomHexColor());

                // 3. Enqueue into first queue (Q0)
                outputQueue.put(product);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stopGenerator() {
        running = false;
    }
}


