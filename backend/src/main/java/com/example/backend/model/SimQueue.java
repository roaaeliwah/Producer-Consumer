package com.example.backend.model;

import com.example.backend.observer.QueueObserver;
import com.example.backend.observer.QueueSubject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class SimQueue implements QueueSubject {
    private final String id;
    private LinkedBlockingQueue<Product> products = new LinkedBlockingQueue<>();
    private List<QueueObserver> observers = new ArrayList<>();
    public SimQueue(String id) {
        this.id = id;
    }

    public synchronized void put(Product product){ // add the product
        products.add(product);
        notifyObservers();
    }

    public synchronized Product take() {
        return products.poll(); // returns null if empty
    }
    public synchronized void attach(QueueObserver observer){ // add the ready observers
        if(!observers.contains(observer)){
            observers.add(observer);
        }
    }

    @Override
    public void detach(QueueObserver observer) { // remove observer when not ready
        observers.remove(observer);
    }
    public void notifyObservers(){ // notify the first empty observer
        if(!observers.isEmpty() && !products.isEmpty()){
            QueueObserver firstReadyMachine = observers.remove(0);
            firstReadyMachine.update();
        }
    }
    public int size() {
        return products.size();
    }

    public int getId() {
        return this.id;
    }


}
