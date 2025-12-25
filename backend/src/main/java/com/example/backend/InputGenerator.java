package com.example.backend;

import com.example.backend.model.Product;
import com.example.backend.model.SimQueue;

public class InputGenerator implements Runnable {

    private final SimQueue inputQueue;
    private int productCounter = 0; // to assign unique IDs to products
    private volatile boolean running = true;

    public InputGenerator(SimQueue inputQueue) {
        this.inputQueue = inputQueue;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(300 + (int)(Math.random() * 700));
                Product p = new Product(productCounter++, randomColor());
                inputQueue.put(p);
                System.out.println("Generated product " + p.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String randomColor() {
        return "#" + Integer.toHexString((int)(Math.random() * 0xFFFFFF));
    }

    public void stop() {
        running = false;
    }
}

