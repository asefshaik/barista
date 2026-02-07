package com.barista.util;

import com.barista.model.DrinkType;
import com.barista.model.Order;
import com.barista.model.OrderStatus;
import com.barista.model.Barista;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Utility class for simulating customer orders during peak hours
 * Helps test the system with realistic traffic patterns
 */
public class OrderSimulator {

    private static final Random random = new Random();
    private static final String[] CUSTOMER_NAMES = {
            "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank",
            "Grace", "Henry", "Ivy", "Jack", "Kate", "Liam"
    };

    private static final String[] LOYALTY_STATUSES = {"NONE", "BRONZE", "SILVER", "GOLD"};

    /**
     * Generate a random order based on drink frequency distribution
     */
    public static Order generateRandomOrder() {
        Order order = new Order();
        order.setCustomerName(CUSTOMER_NAMES[random.nextInt(CUSTOMER_NAMES.length)]);
        order.setIsRegularCustomer(random.nextDouble() < 0.6); // 60% regular customers
        order.setLoyaltyStatus(randomLoyaltyStatus());
        
        // Generate 1-3 drinks per order
        int drinkCount = random.nextInt(3) + 1;
        List<String> drinks = new ArrayList<>();
        int totalPrepTime = 0;
        
        for (int i = 0; i < drinkCount; i++) {
            String drink = selectDrinkByFrequency();
            drinks.add(drink);
            totalPrepTime += DrinkType.valueOf(drink).getPrepTime();
        }
        
        order.setDrinks(drinks);
        order.setTotalPrepTime(totalPrepTime);
        order.setStatus(OrderStatus.WAITING);
        order.setCreatedAt(LocalDateTime.now());
        
        return order;
    }

    /**
     * Select a drink based on the given frequency distribution
     */
    private static String selectDrinkByFrequency() {
        double random_value = random.nextDouble();
        
        // Cumulative frequency
        if (random_value < 0.25) return "COLD_BREW";
        if (random_value < 0.45) return "ESPRESSO";
        if (random_value < 0.60) return "AMERICANO";
        if (random_value < 0.80) return "CAPPUCCINO";
        if (random_value < 0.92) return "LATTE";
        return "SPECIALTY_MOCHA";
    }

    private static String randomLoyaltyStatus() {
        return LOYALTY_STATUSES[random.nextInt(LOYALTY_STATUSES.length)];
    }

    /**
     * Simulate Poisson arrival distribution
     * λ = 1.4 customers per minute (from requirements)
     */
    public static long nextArrivalTime() {
        double lambda = 1.4 / 60.0; // customers per second
        double u = random.nextDouble();
        long arrivalTimeMs = (long) (-Math.log(1.0 - u) / lambda * 1000);
        return Math.max(100, arrivalTimeMs); // At least 100ms
    }

    /**
     * Generate test baristas
     */
    public static List<Barista> generateTestBaristas() {
        List<Barista> baristas = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Barista barista = new Barista();
            barista.setId(i);
            barista.setName("Barista " + i);
            barista.setTotalWorkTime(0);
            barista.setCompletedOrders(0);
            barista.setWorkloadRatio(0.0);
            baristas.add(barista);
        }
        return baristas;
    }

    /**
     * Simulate a batch of orders for testing
     */
    public static List<Order> generateOrderBatch(int count) {
        List<Order> orders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < count; i++) {
            Order order = generateRandomOrder();
            order.setId((long)(i + 1));
            // Stagger arrival times
            order.setCreatedAt(now.minusSeconds(count - i - 1));
            orders.add(order);
        }
        
        return orders;
    }

    /**
     * Calculate statistics for a set of completed orders
     */
    public static Map<String, Object> calculateStatistics(List<Order> orders) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Order> completed = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .toList();
        
        if (completed.isEmpty()) {
            stats.put("avgWaitTime", 0);
            stats.put("maxWaitTime", 0);
            stats.put("minWaitTime", 0);
            return stats;
        }
        
        // Calculate average wait time
        double avgWaitTime = completed.stream()
                .mapToLong(o -> java.time.temporal.ChronoUnit.SECONDS.between(o.getCreatedAt(), o.getCompletedAt()))
                .average()
                .orElse(0);
        
        long maxWaitTime = completed.stream()
                .mapToLong(o -> java.time.temporal.ChronoUnit.SECONDS.between(o.getCreatedAt(), o.getCompletedAt()))
                .max()
                .orElse(0);
        
        long minWaitTime = completed.stream()
                .mapToLong(o -> java.time.temporal.ChronoUnit.SECONDS.between(o.getCreatedAt(), o.getCompletedAt()))
                .min()
                .orElse(0);
        
        stats.put("avgWaitTime", (int) avgWaitTime);
        stats.put("maxWaitTime", maxWaitTime);
        stats.put("minWaitTime", minWaitTime);
        
        return stats;
    }
}
