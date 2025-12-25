package com.example.backend.model;

import lombok.Data;
import lombok.Getter;

@Data
public class Product {
    private final int id;
    private final String color;

    public Product(int id, String color) {
        this.id = id;
        this.color = color;
    }
}
