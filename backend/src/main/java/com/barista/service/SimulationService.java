package com.barista.service;

import com.barista.model.*;
import com.barista.util.BaristaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SimulationService {

    @Autowired
    private BaristaRepository baristaRepository;

    private List<TestCaseResult> testResults = new CopyOnWriteArrayList<>();
    private boolean simulationRunning = false;

    private static final String[] DRINK_TYPES = { "COLD_BREW", "ESPRESSO", "AMERICANO", "CAPPUCCINO", "LATTE",
            "SPECIALTY_MOCHA" };
    private static final String[] LOYALTY_STATUSES = { "NONE", "BRONZE", "SILVER", "GOLD" };
    private static final String[] CUSTOMER_NAMES = { "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace",
            "Henry", "Ivy", "Jack" };

    private Random random = new Random();

    public List<TestCaseResult> runAllTestCases() {
        if (simulationRunning) {
            return testResults;
        }

        simulationRunning = true;
        testResults.clear();

        try {
            for (int i = 1; i <= 10; i++) {
                TestCaseResult result = runSingleTestCase(i);
                testResults.add(result);
            }
        } finally {
            simulationRunning = false;
        }

        return testResults;
    }

    public TestCaseResult runSingleTestCase(int testCaseNumber) {
        // Simulation now runs in isolation without wiping real orders
        resetBaristas();

        // Extended window: 3 hours = 180 minutes (peak shift)
        // 3 baristas × 180 min = 540 min capacity, ~120-180 orders × 3.5 min = 420-630
        // min
        // 120-180 is balanced for 3 baristas in 3 hours to stay under 5m wait
        int orderCount = 120 + random.nextInt(61); // 120-180 orders
        List<SimulatedOrder> simulatedOrders = new ArrayList<>();

        LocalDateTime startTime = LocalDateTime.now().minusHours(3);

        for (int i = 0; i < orderCount; i++) {
            SimulatedOrder simOrder = generateRandomOrder(startTime, i, orderCount);
            simulatedOrders.add(simOrder);
        }

        // Sort by arrival time
        simulatedOrders.sort(Comparator.comparing(o -> o.arrivalTime));

        // Process orders through the queue simulation with priority-based assignment
        Map<Integer, LocalDateTime> baristaFreeAt = new HashMap<>();
        baristaFreeAt.put(1, startTime);
        baristaFreeAt.put(2, startTime);
        baristaFreeAt.put(3, startTime);

        // Track barista workload for load balancing
        Map<Integer, Integer> baristaWorkMinutes = new HashMap<>();
        baristaWorkMinutes.put(1, 0);
        baristaWorkMinutes.put(2, 0);
        baristaWorkMinutes.put(3, 0);

        int complaints = 0;
        int abandoned = 0;
        long totalWaitSeconds = 0;
        int[] baristaOrderCounts = { 0, 0, 0 };
        List<TestCaseResult.OrderDetail> orderDetails = new ArrayList<>();

        int orderNum = 1;

        for (SimulatedOrder order : simulatedOrders) {
            // Algorithm: Assign to barista with LOWEST workload who is available soonest
            int bestBarista = findBestBarista(baristaFreeAt, baristaWorkMinutes, order.arrivalTime);
            LocalDateTime earliestFree = baristaFreeAt.get(bestBarista);

            // Smart scheduling: If barista is busy, check if another is available
            for (int b = 1; b <= 3; b++) {
                LocalDateTime bFree = baristaFreeAt.get(b);
                if (!bFree.isAfter(order.arrivalTime)) {
                    bestBarista = b;
                    earliestFree = bFree;
                    break;
                }
            }

            // Calculate when this order starts
            LocalDateTime orderStartTime = order.arrivalTime.isAfter(earliestFree)
                    ? order.arrivalTime
                    : earliestFree;

            // Calculate wait time
            long waitSeconds = ChronoUnit.SECONDS.between(order.arrivalTime, orderStartTime);

            // ABANDONMENT LOGIC:
            // - New customer (isRegular = false): abandons after 8 min (480 sec)
            // - Old customer (isRegular = true): abandons after 10 min (600 sec) +
            // complaint
            boolean isRegular = order.isRegular;
            int abandonThreshold = isRegular ? 600 : 480; // 10 min for old, 8 min for new

            String status = "COMPLETED";
            if (waitSeconds >= abandonThreshold) {
                abandoned++;
                status = "ABANDONED";

                // Old customer abandonment = complaint to manager
                if (isRegular && waitSeconds >= 600) {
                    complaints++;
                    status = "COMPLAINT";
                }
            } else {
                // Order was served - update barista state
                totalWaitSeconds += waitSeconds;
                LocalDateTime orderEndTime = orderStartTime.plusMinutes(order.prepTime);
                baristaFreeAt.put(bestBarista, orderEndTime);
                baristaWorkMinutes.put(bestBarista, baristaWorkMinutes.get(bestBarista) + order.prepTime);
                baristaOrderCounts[bestBarista - 1]++;
            }

            // Record order details with relative timestamp (t+Xm)
            long mins = ChronoUnit.MINUTES.between(startTime, order.arrivalTime);
            String relativeTimeLabel = "t+" + mins + "m";

            TestCaseResult.OrderDetail detail = new TestCaseResult.OrderDetail(
                    orderNum++,
                    order.customerName,
                    order.loyaltyStatus,
                    isRegular,
                    status.equals("ABANDONED") || status.equals("COMPLAINT") ? null : bestBarista,
                    waitSeconds,
                    status,
                    order.drinks,
                    relativeTimeLabel);
            orderDetails.add(detail);

            order.assignedBarista = bestBarista;
            order.waitSeconds = waitSeconds;
        }

        int servedOrders = orderCount - abandoned;
        double avgWaitTime = servedOrders > 0 ? (double) totalWaitSeconds / servedOrders : 0;
        double timeoutRate = orderCount > 0 ? (complaints * 100.0 / orderCount) : 0;
        double abandonmentRate = orderCount > 0 ? (abandoned * 100.0 / orderCount) : 0;

        TestCaseResult result = new TestCaseResult(
                testCaseNumber,
                orderCount,
                avgWaitTime,
                baristaOrderCounts[0],
                baristaOrderCounts[1],
                baristaOrderCounts[2],
                complaints,
                abandoned,
                timeoutRate,
                abandonmentRate);
        result.setOrders(orderDetails);

        return result;
    }

    // Find the best barista using workload balancing (simulates actual algorithm)
    private int findBestBarista(Map<Integer, LocalDateTime> baristaFreeAt,
            Map<Integer, Integer> baristaWorkMinutes,
            LocalDateTime orderArrival) {
        int bestBarista = 1;
        double bestScore = Double.MAX_VALUE;

        for (int b = 1; b <= 3; b++) {
            LocalDateTime freeAt = baristaFreeAt.get(b);
            int workload = baristaWorkMinutes.get(b);

            // Calculate score: prioritize available baristas with lower workload
            long waitSeconds = 0;
            if (freeAt.isAfter(orderArrival)) {
                waitSeconds = ChronoUnit.SECONDS.between(orderArrival, freeAt);
            }

            // Score = wait time penalty + workload imbalance penalty
            double score = waitSeconds + (workload * 10); // Penalize high workload

            if (score < bestScore) {
                bestScore = score;
                bestBarista = b;
            }
        }

        return bestBarista;
    }

    private SimulatedOrder generateRandomOrder(LocalDateTime startTime, int orderIndex, int totalOrders) {
        SimulatedOrder order = new SimulatedOrder();

        // Distribute arrivals EVENLY over 3-hour window (180 minutes)
        // This ensures baristas have capacity and wait times stay low
        double progress = (double) orderIndex / totalOrders;
        int minutesOffset = (int) (progress * 180);

        // Add small randomness (±2 minutes) to simulate natural variance
        minutesOffset += random.nextInt(5) - 2;
        order.arrivalTime = startTime.plusMinutes(Math.max(0, Math.min(179, minutesOffset)));

        // Random drinks (1-2 drinks per order for realistic average prep time)
        int drinkCount = 1 + random.nextInt(2); // 1-2 drinks
        order.drinks = new ArrayList<>();
        int totalPrepTime = 0;

        for (int d = 0; d < drinkCount; d++) {
            String drink = DRINK_TYPES[random.nextInt(DRINK_TYPES.length)];
            order.drinks.add(drink);
            totalPrepTime += getDrinkPrepTime(drink);
        }

        order.prepTime = totalPrepTime;
        order.customerName = CUSTOMER_NAMES[random.nextInt(CUSTOMER_NAMES.length)] + " " + (orderIndex + 1);
        order.loyaltyStatus = LOYALTY_STATUSES[random.nextInt(LOYALTY_STATUSES.length)];
        // Regular (old) customers have SILVER or GOLD loyalty - they wait longer but
        // file complaints
        order.isRegular = "SILVER".equals(order.loyaltyStatus) || "GOLD".equals(order.loyaltyStatus);

        return order;
    }

    private int getDrinkPrepTime(String drink) {
        switch (drink) {
            case "COLD_BREW":
                return 1;
            case "ESPRESSO":
                return 2;
            case "AMERICANO":
                return 2;
            case "CAPPUCCINO":
                return 4;
            case "LATTE":
                return 4;
            case "SPECIALTY_MOCHA":
                return 6;
            default:
                return 3;
        }
    }

    private void resetBaristas() {
        List<Barista> baristas = baristaRepository.findAll();
        for (Barista barista : baristas) {
            barista.setCurrentOrderId(null);
            barista.setBusyUntil(null);
            barista.setTotalWorkTime(0);
            barista.setCompletedOrders(0);
            barista.setWorkloadRatio(0.0);
        }
        baristaRepository.saveAll(baristas);
    }

    public List<TestCaseResult> getResults() {
        return new ArrayList<>(testResults);
    }

    public boolean isRunning() {
        return simulationRunning;
    }

    // Inner class for simulation
    private static class SimulatedOrder {
        LocalDateTime arrivalTime;
        List<String> drinks;
        int prepTime;
        String customerName;
        String loyaltyStatus;
        boolean isRegular; // true = old customer (10 min patience), false = new (8 min)
        int assignedBarista;
        long waitSeconds;
    }
}
