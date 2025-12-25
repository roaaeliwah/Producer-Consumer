package com.example.backend.model;

import com.example.backend.observer.QueueObserver;
import com.example.backend.observer.QueueSubject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class SimQueue implements QueueSubject {
    private final String id = UUID.randomUUID().toString();
    private LinkedBlockingQueue<Product> products = new LinkedBlockingQueue<>();
    private List<QueueObserver> observers = new ArrayList<>();

    public synchronized void put(Product product){ // add the product
        products.add(product);
        notifyObservers();
    }

    public synchronized Product take() {
        return products.poll(); // returns null if empty
    }

    @Override
    public synchronized void attach(QueueObserver observer){ // add the ready observers
        if(!observers.contains(observer)){
            observers.add(observer);
        }
    }

    @Override
    public void detach(QueueObserver observer) { // remove observer when not ready
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(){ // notify the first empty observer
        if(!observers.isEmpty() && !products.isEmpty()){
            QueueObserver firstReadyMachine = observers.remove(0);
            firstReadyMachine.update();
        }
    }

    public int size() {
        return products.size();
    }
}
