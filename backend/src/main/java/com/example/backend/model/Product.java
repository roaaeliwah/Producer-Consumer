package com.example.backend.model;

import com.example.backend.util.ColorGenerator;
import lombok.Data;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Product {
    private final String id = UUID.randomUUID().toString();
    private ColorGenerator colorGenerator = new ColorGenerator();
    private final String color = colorGenerator.randomHexColor();
}
