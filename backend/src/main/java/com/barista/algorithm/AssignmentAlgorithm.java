package com.barista.algorithm;

import com.barista.model.Barista;
import com.barista.model.Order;
import com.barista.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AssignmentAlgorithm {

    public static Barista findBestBarista(Order order, List<Barista> baristas) {
        // Filter available baristas
        List<Barista> availableBaristas = baristas.stream()
                .filter(b -> !b.isBusy())
                .collect(Collectors.toList());

        if (availableBaristas.isEmpty()) {
            // Return barista who will be free soonest
            return baristas.stream()
                    .min(Comparator.comparingInt(Barista::getEstimatedFreeTime))
                    .orElse(null);
        }

        // Score each available barista
        Barista bestBarista = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Barista barista : availableBaristas) {
            double score = calculateBaristaScore(barista, order);
            if (score > bestScore) {
                bestScore = score;
                bestBarista = barista;
            }
        }

        return bestBarista;
    }

    private static double calculateBaristaScore(Barista barista, Order order) {
        double score = 100.0;

        // Workload balancing: prefer underutilized baristas
        // Overloaded baristas should prefer quick orders
        if (barista.getWorkloadRatio() > 1.2) {
            // Overloaded - give bonus for quick orders
            if (order.getTotalPrepTime() <= 2) {
                score += 50;
            } else if (order.getTotalPrepTime() <= 4) {
                score += 20;
            } else {
                score -= 30;
            }
        } else if (barista.getWorkloadRatio() < 0.8) {
            // Underutilized - can take any order
            score += 30;
        }

        return score;
    }

    public static List<Order> rankOrdersByPriority(List<Order> waitingOrders, List<Order> completedOrders) {
        return waitingOrders.stream()
                .sorted((o1, o2) -> {
                    int score1 = PriorityScoreCalculator.calculatePriorityScore(o1, waitingOrders, completedOrders);
                    int score2 = PriorityScoreCalculator.calculatePriorityScore(o2, waitingOrders, completedOrders);
                    return score2 - score1; // Descending order
                })
                .collect(Collectors.toList());
    }

    public static void assignOrdersToBaristas(List<Order> waitingOrders, List<Barista> baristas,
            List<Order> completedOrders) {
        // Rank orders by priority
        List<Order> rankedOrders = rankOrdersByPriority(waitingOrders, completedOrders);

        // Assign each order to best available barista
        for (Order order : rankedOrders) {
            if (order.getStatus() != OrderStatus.PREPARING) {
                Barista barista = findBestBarista(order, baristas);
                if (barista != null) {
                    order.setAssignedToBarista(barista.getId());
                    order.setPriorityScore(
                            PriorityScoreCalculator.calculatePriorityScore(order, waitingOrders, completedOrders));
                }
            }
        }
    }

    public static void updateWorkloadRatios(List<Barista> baristas) {
        if (baristas.isEmpty())
            return;

        double averageWorkload = baristas.stream()
                .mapToInt(Barista::getTotalWorkTime)
                .average()
                .orElse(0);

        for (Barista barista : baristas) {
            if (averageWorkload > 0) {
                barista.setWorkloadRatio((double) barista.getTotalWorkTime() / averageWorkload);
            }
        }
    }
}
