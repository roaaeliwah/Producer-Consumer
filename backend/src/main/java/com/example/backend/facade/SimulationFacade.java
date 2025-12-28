package com.example.backend.facade;

import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.ObjectInitDTO;
import com.example.backend.service.SimulationStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulationFacade {
    @Autowired
    private SimulationStateService stateService;

    public void initializeObjects(ObjectInitDTO initData) {

        stateService.reset();

        if (initData.getQueues() != null) {
            for (String queueId : initData.getQueues()) {
                stateService.addQueue(queueId);
            }
        }
        if (initData.getMachines() != null) {
            for (String machineId : initData.getMachines()) {
                stateService.addMachine(machineId);
            }
        }
    }

    public void connectComponents(List<ConnectionDTO> connections) {
        if (connections == null)
            return;

        for (ConnectionDTO conn : connections) {
            if ("INPUT".equalsIgnoreCase(conn.getType())) {
                stateService.connectInputQueue(conn.getMachineId(), conn.getQueueId());
            } else if ("OUTPUT".equalsIgnoreCase(conn.getType())) {
                stateService.connectOutputQueue(conn.getMachineId(), conn.getQueueId());
            }
        }
    }
}
