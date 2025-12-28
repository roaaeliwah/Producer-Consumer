package com.example.backend.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionDTO {
    private String machineId;
    private String queueId;
    private String type;
}
