package com.example.backend.observer;

public interface QueueSubject {
    void attach(QueueObserver observer);
    void detach(QueueObserver observer);
    void notifyObservers();

}
