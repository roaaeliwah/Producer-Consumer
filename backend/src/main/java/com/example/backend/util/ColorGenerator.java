package com.example.backend.util;

import java.util.Random;

public class ColorGenerator {

    private static final Random random = new Random();

    public String randomHexColor() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);

        return String.format("#%02X%02X%02X", r, g, b);
    }
}
