package com.barista.algorithm;

import com.barista.model.Order;
import com.barista.model.OrderStatus;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PriorityScoreCalculator {

    private static final double WAIT_TIME_WEIGHT = 0.40;
    private static final double COMPLEXITY_WEIGHT = 0.25;
    private static final double LOYALTY_WEIGHT = 0.10;
    private static final double URGENCY_WEIGHT = 0.25;

    private static final int MAX_WAIT_TIME_SECONDS = 10 * 60; // 10 minutes
    private static final int EMERGENCY_THRESHOLD_SECONDS = 8 * 60; // 8 minutes
    private static final int EMERGENCY_BOOST = 50;

    private static final int FAIRNESS_SKIP_THRESHOLD = 3; // Allow skipping 3 customers
    private static final int FAIRNESS_PENALTY = 30; // Penalty if exceeds threshold

    public static int calculatePriorityScore(Order order, List<Order> allWaitingOrders, List<Order> completedOrders) {
        double score = 0.0;

        // 1. Wait Time Component (40%)
        long secondsWaiting = ChronoUnit.SECONDS.between(order.getCreatedAt(), LocalDateTime.now());
        double waitTimeScore = Math.min(100, (secondsWaiting / (double) MAX_WAIT_TIME_SECONDS) * 100);
        score += waitTimeScore * WAIT_TIME_WEIGHT;

        // 2. Complexity Component (25%) - shorter prep time = higher score
        int totalPrepTime = order.getTotalPrepTime() * 60; // Convert to seconds
        double complexityScore = Math.max(0, 100 - ((totalPrepTime / (double) (6 * 60)) * 100)); // 6 min is max
        score += complexityScore * COMPLEXITY_WEIGHT;

        // 3. Loyalty Component (10%)
        double loyaltyScore = getLoyaltyScore(order);
        score += loyaltyScore * LOYALTY_WEIGHT;

        // 4. Urgency Component (25%)
        double urgencyScore = calculateUrgency(secondsWaiting);
        score += urgencyScore * URGENCY_WEIGHT;

        // Emergency Handling - if approaching 8 minutes
        if (secondsWaiting >= EMERGENCY_THRESHOLD_SECONDS) {
            score += EMERGENCY_BOOST;
        }

        // Fairness Enforcement - penalty if too many people skipped
        int skippedCount = countSkippedCustomers(order, allWaitingOrders, completedOrders);
        if (skippedCount > FAIRNESS_SKIP_THRESHOLD) {
            score += FAIRNESS_PENALTY;
        }

        return Math.min(100, (int) score);
    }

    public static String getPriorityReason(Order order, List<Order> allWaitingOrders, List<Order> completedOrders) {
        List<String> reasons = new ArrayList<>();
        long secondsWaiting = ChronoUnit.SECONDS.between(order.getCreatedAt(), LocalDateTime.now());

        // Check emergency
        if (secondsWaiting >= EMERGENCY_THRESHOLD_SECONDS) {
            reasons.add("⚠️ Emergency: waiting 8+ min");
        }

        // Check fairness
        int skippedCount = countSkippedCustomers(order, allWaitingOrders, completedOrders);
        if (skippedCount > FAIRNESS_SKIP_THRESHOLD) {
            reasons.add("📊 Fairness boost: " + skippedCount + " orders passed");
        } else if (skippedCount > 0) {
            reasons.add(skippedCount + " faster order(s) served ahead");
        }

        // Check loyalty
        if (order.getLoyaltyStatus() != null && !order.getLoyaltyStatus().equals("NONE")) {
            reasons.add("👑 " + order.getLoyaltyStatus() + " member");
        }

        // Check complexity
        if (order.getTotalPrepTime() <= 2) {
            reasons.add("☕ Quick order (≤2 min)");
        } else if (order.getTotalPrepTime() >= 5) {
            reasons.add("🍵 Complex order (" + order.getTotalPrepTime() + " min)");
        }

        if (reasons.isEmpty()) {
            return "Standard queue position";
        }
        return String.join(" | ", reasons);
    }

    private static double getLoyaltyScore(Order order) {
        if (order.getLoyaltyStatus() == null)
            return 0;
        return switch (order.getLoyaltyStatus()) {
            case "GOLD" -> 30;
            case "SILVER" -> 20;
            case "BRONZE" -> 10;
            default -> 0;
        };
    }

    private static double calculateUrgency(long secondsWaiting) {
        if (secondsWaiting >= EMERGENCY_THRESHOLD_SECONDS) {
            return 100; // Maximum urgency
        }
        return (secondsWaiting / (double) EMERGENCY_THRESHOLD_SECONDS) * 80;
    }

    private static int countSkippedCustomers(Order currentOrder, List<Order> allWaitingOrders,
            List<Order> completedOrders) {
        int skipped = 0;
        for (Order completed : completedOrders) {
            if (completed.getCreatedAt().isAfter(currentOrder.getCreatedAt()) &&
                    completed.getCompletedAt().isBefore(LocalDateTime.now())) {
                skipped++;
            }
        }
        return skipped;
    }

    public static boolean checkTimeoutRisk(Order order) {
        long secondsWaiting = ChronoUnit.SECONDS.between(order.getCreatedAt(), LocalDateTime.now());
        return secondsWaiting >= MAX_WAIT_TIME_SECONDS;
    }

    public static boolean checkAbandonmentRisk(Order order, boolean isRegular) {
        long secondsWaiting = ChronoUnit.SECONDS.between(order.getCreatedAt(), LocalDateTime.now());
        int abandonmentThreshold = isRegular ? 600 : 480; // 10 min vs 8 min
        return secondsWaiting >= abandonmentThreshold;
    }
}
