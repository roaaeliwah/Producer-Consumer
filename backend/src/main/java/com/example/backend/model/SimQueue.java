package com.example.backend.model;

import com.example.backend.observer.QueueObserver;
import com.example.backend.observer.QueueSubject;
import com.example.backend.service.SimulationService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class SimQueue implements QueueSubject {

    private Runnable onUpdate;
    private String id;
    private LinkedBlockingQueue<Product> products = new LinkedBlockingQueue<>();
    private List<QueueObserver> observers = new ArrayList<>();

    public  SimQueue(String id) {
        this.id = id;
    }

    public synchronized void put(Product product){
        products.add(product);
        notifyObservers();
        if(onUpdate != null) onUpdate.run();
    }

    public synchronized Product take() {
        Product p = products.poll();
        if (p != null) {
            if(onUpdate != null) onUpdate.run();
        }
        return p;
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
    public void notifyObservers(){           // notify the first empty observer
        if(!observers.isEmpty() && !products.isEmpty()){
            QueueObserver firstReadyMachine = observers.remove(0);
            firstReadyMachine.update();
        }
    }

    public int size() {
        return products.size();
    }
}
