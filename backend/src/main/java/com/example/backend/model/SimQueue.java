package com.example.backend.model;

import lombok.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class SimQueue {
    private final String id;
    private BlockingQueue<Product> buffer;

    public SimQueue(String id) {
        this.id = id;
        this.buffer = new LinkedBlockingQueue<>();
    }

    public void put(Product product) throws InterruptedException {
        buffer.put(product);
    }

    public Product take() throws InterruptedException {
        return buffer.take(); // blocks if empty
    }

    public int size() {
        return buffer.size();
    }
}
