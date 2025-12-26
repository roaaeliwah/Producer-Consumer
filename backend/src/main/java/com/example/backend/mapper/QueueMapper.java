package com.example.backend.mapper;

import com.example.backend.dto.QueueDTO;
import com.example.backend.model.Product;
import com.example.backend.model.SimQueue;

import java.util.List;

public class QueueMapper {
    public QueueDTO toDTO (SimQueue queue) {
        List<String> productColors = queue.getProducts()
                .stream()
                .map(Product::getColor)
                .toList();

        return new QueueDTO(queue.getId(), queue.size(), productColors);
    }
}
