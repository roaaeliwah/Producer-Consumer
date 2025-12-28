package com.example.backend.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
public class QueueDTO {
    private String id;
    private int size;
    // Colors of products for visualization
    private List<String> productColors;
}
