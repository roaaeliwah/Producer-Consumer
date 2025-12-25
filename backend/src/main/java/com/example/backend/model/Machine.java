package com.example.backend.model;

import lombok.Data;

import java.util.List;

@Data
public class Machine implements Runnable{
    private final int id;
    private String status = "IDLE";
    private String currentColor = "GRAY";
    private List<SimQueue> inputSimQueue;
    private List<SimQueue> outputSimQueue;
    private volatile boolean running = true;

    public Machine(int id, List<SimQueue> inputSimQueue, List<SimQueue> outputSimQueue) {
        this.id = id;
        this.inputSimQueue = inputSimQueue;
        this.outputSimQueue = outputSimQueue;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Product product = inputSimQueue.take(); // blocks
                System.out.println(id + " processing product " + product.getId());

                int serviceTime = 500 + (int)(Math.random() * 1000);
                Thread.sleep(serviceTime);

                outputSimQueue.put(product);
                System.out.println(id + " finished product " + product.getId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
