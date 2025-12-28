package com.example.backend.dto;

import com.example.backend.service.SimulationMode;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimStateDTO {
    private long timestamp;
    private List<MachineDTO> machines;
    private List<QueueDTO> queues;
    private SimulationMode mode;
    private List<ConnectionDTO> connections;
}

