package com.example.backend.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MachineDTO {
    private String id;
    private String state;
    // Color of the product being processed
    private String currentColor;
    private List<String> inputQueueIds;
    private List<String> outputQueueIds;
}
