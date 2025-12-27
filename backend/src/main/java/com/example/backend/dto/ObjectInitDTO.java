package com.example.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ObjectInitDTO {
    private List<String> queues;
    private List<String> machines;
}