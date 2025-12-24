package com.example.backend.model;

import lombok.Data;

@Data
public class Machine {
    private final int id;
    private String status = "IDLE";
    private String currentColor = "GRAY";

    public Machine(int id) {
        this.id = id;
    }
}
