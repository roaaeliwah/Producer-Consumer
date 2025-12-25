package com.example.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class MachineDTO {
    private String id;               // Machine ID
    private String state;            // "idle", "processing", etc.
    private String currentColor;     // Color of the product being processed
    private List<String> inputQueueIds;   // IDs of input queues
    private List<String> outputQueueIds;  // IDs of output queues

    // Constructor
    public MachineDTO(String id, String state, String currentColor,
                      List<String> inputQueueIds, List<String> outputQueueIds) {
        this.id = id;
        this.state = state;
        this.currentColor = currentColor;
        this.inputQueueIds = inputQueueIds;
        this.outputQueueIds = outputQueueIds;
    }
}
