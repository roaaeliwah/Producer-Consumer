package com.example.backend.service;

import com.example.backend.dto.SimStateDTO;
import com.example.backend.mapper.SimStateMapper;
import com.example.backend.model.Machine;
import com.example.backend.model.SimQueue;
import com.example.backend.snapshot.SimulationSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

@Service
public class SsePublisherService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        System.out.println("Creating emitter");
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // no timeout, the connection stays alive indefinitely
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    public void publishSnapshot(SimulationSnapshot snapshot, Map<String, Machine> machines,
            Map<String, SimQueue> queues, SimulationMode mode) {
        SimStateDTO dto = SimStateMapper.toDTO(snapshot, machines, queues, mode);
        emitters.removeIf(emitter -> {
            try {
                emitter.send(dto);
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }

    public void notifySimulationStopped() {
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("simulationStopped")
                        .data("STOPPED"));
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }
}
