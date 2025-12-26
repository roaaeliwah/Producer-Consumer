package com.example.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueueDTO {
    private String id;          // Queue ID
    private int size;           // Number of products currently in the queue
    private List<String> productColors; // Colors of products for visualization

    // Constructor
    public QueueDTO(String id, int size, List<String> productColors) {
        this.id = id;
        this.size = size;
        this.productColors = productColors;
    }
}
