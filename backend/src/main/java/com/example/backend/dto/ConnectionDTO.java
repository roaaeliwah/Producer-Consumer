package com.example.backend.dto;

import lombok.Data;

@Data
public class ConnectionDTO {
    private String machineId;
    private String queueId;

    private String type;        // "INPUT" or "OUTPUT"

    public ConnectionDTO() {}

    // Constructor
    public ConnectionDTO(String machineId, String queueId, String type) {
        this.type = type;
        this.machineId = machineId;
        this.queueId = queueId;
    }
}
