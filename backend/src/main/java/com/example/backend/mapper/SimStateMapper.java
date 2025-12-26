package com.example.backend.mapper;

import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.MachineDTO;
import com.example.backend.dto.QueueDTO;
import com.example.backend.dto.SimStateDTO;
import com.example.backend.model.Machine;
import com.example.backend.model.MachineState;
import com.example.backend.model.Product;
import com.example.backend.model.SimQueue;
import com.example.backend.service.SimulationMode;
import com.example.backend.snapshot.SimulationSnapshot;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimStateMapper {
    public static SimStateDTO toDTO(SimulationSnapshot snapshot,
                                    Map<String, Machine> machines,
                                    Map<String, SimQueue> queues,
                                    SimulationMode mode) {

        Map<String, String> machineColors = snapshot.getMachineColors();
        Map<String, String> machineStates = snapshot.getMachineStates();
        Map<String, Integer> queueSizes = snapshot.getQueueSizes();

        List<MachineDTO> machineDTOs = machines.values().stream()
                .map(m -> new MachineDTO(
                        m.getId(),
                        machineStates.getOrDefault(m.getId(), String.valueOf(MachineState.IDLE)),
                        machineColors.getOrDefault(m.getId(), null),
                        m.getInputQueues().stream().map(SimQueue::getId).toList(),
                        m.getOutputQueue() != null ? List.of(m.getOutputQueue().getId()) : List.of()
                ))
                .toList();


        List<QueueDTO> queueDTOs = queues.values().stream()
                .map(q -> new QueueDTO(
                        q.getId(),
                        queueSizes.getOrDefault(q.getId(), 0),
                        q.getProducts().stream().map(Product::getColor).toList()
                ))
                .toList();

        List<ConnectionDTO> connectionDTOs = new java.util.ArrayList<>(machines.values().stream()
                .flatMap(m -> m.getInputQueues().stream()
                        .map(q -> new ConnectionDTO(m.getId(), q.getId(), "INPUT"))
                )
                .toList());


        connectionDTOs.addAll(
                machines.values().stream()
                        .filter(m -> m.getOutputQueue() != null)
                        .map(m -> new ConnectionDTO(m.getId(), m.getOutputQueue().getId(), "OUTPUT"))
                        .toList()
        );

        return new SimStateDTO(
                snapshot.getTimestamp(),
                machineDTOs,
                queueDTOs,
                mode,
                connectionDTOs
        );

    }
}

