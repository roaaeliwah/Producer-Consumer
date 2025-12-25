package com.example.backend.model;

import lombok.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class SimQueue {
    private final int id;
    private LinkedBlockingQueue<Product> queue = new LinkedBlockingQueue<>();

    public SimQueue(int id) {
        this.id = id;
    }

    public void put(Product product) throws InterruptedException {
        queue.put(product);
    }

    public Product take() throws InterruptedException {
        return queue.take(); // blocks if empty
    }

    public int size() {
        return queue.size();
    }
}
