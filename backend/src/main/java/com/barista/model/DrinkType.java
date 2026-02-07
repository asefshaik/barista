package com.barista.model;

public enum DrinkType {
    COLD_BREW("Cold Brew", 1, 0.25, 120),
    ESPRESSO("Espresso", 2, 0.20, 150),
    AMERICANO("Americano", 2, 0.15, 140),
    CAPPUCCINO("Cappuccino", 4, 0.20, 180),
    LATTE("Latte", 4, 0.12, 200),
    SPECIALTY_MOCHA("Specialty (Mocha)", 6, 0.08, 250);

    private final String displayName;
    private final int prepTime; // in minutes
    private final double frequency; // percentage
    private final int price; // in rupees

    DrinkType(String displayName, int prepTime, double frequency, int price) {
        this.displayName = displayName;
        this.prepTime = prepTime;
        this.frequency = frequency;
        this.price = price;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPrepTime() {
        return prepTime;
    }

    public double getFrequency() {
        return frequency;
    }

    public int getPrice() {
        return price;
    }
}
