package com.barista.service;

import com.barista.algorithm.AssignmentAlgorithm;
import com.barista.algorithm.PriorityScoreCalculator;
import com.barista.model.*;
import com.barista.util.BaristaRepository;
import com.barista.util.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueueManagementService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BaristaRepository baristaRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final int RECALCULATION_INTERVAL = 30000; // 30 seconds

    public Order createOrder(List<String> drinks, String customerName, Boolean isRegular, String loyaltyStatus) {
        Order order = new Order();
        order.setDrinks(drinks);
        order.setCustomerName(customerName);
        order.setIsRegularCustomer(isRegular);
        order.setLoyaltyStatus(loyaltyStatus != null ? loyaltyStatus : "NONE");

        // Calculate total prep time
        int totalPrepTime = drinks.stream()
                .mapToInt(drink -> DrinkType.valueOf(drink).getPrepTime())
                .sum();
        order.setTotalPrepTime(totalPrepTime);

        Order savedOrder = orderRepository.save(order);

        // Trigger assignment
        assignOrderToBarista(savedOrder);

        // Broadcast update
        broadcastQueueUpdate();

        return savedOrder;
    }

    public void assignOrderToBarista(Order order) {
        List<Barista> baristas = baristaRepository.findAll();
        if (baristas.isEmpty()) {
            initializeBaristas();
            baristas = baristaRepository.findAll();
        }

        // Update workload ratios
        AssignmentAlgorithm.updateWorkloadRatios(baristas);

        // Find best barista
        Barista barista = AssignmentAlgorithm.findBestBarista(order, baristas);
        if (barista != null) {
            order.setAssignedToBarista(barista.getId());
            System.out.println("[DEBUG] Order " + order.getId() + " assigned to Barista " + barista.getName() + " (ID: "
                    + barista.getId() + ")");

            List<Order> waitingOrders = getWaitingOrders();
            List<Order> completedOrders = getCompletedOrders();

            int priorityScore = PriorityScoreCalculator.calculatePriorityScore(order, waitingOrders, completedOrders);
            order.setPriorityScore(priorityScore);

            // Calculate priority reason
            String priorityReason = PriorityScoreCalculator.getPriorityReason(order, waitingOrders, completedOrders);
            order.setPriorityReason(priorityReason);

            // Calculate estimated wait time based on queue position and barista
            // availability
            int estimatedWait = calculateEstimatedWaitTime(order, barista, waitingOrders);
            order.setEstimatedWaitTime(estimatedWait);

            // AUTO-START: If barista is available (not busy), automatically start the order
            if (!barista.isBusy()) {
                System.out.println("[DEBUG] Barista " + barista.getName() + " is available - AUTO-STARTING order "
                        + order.getId());
                order.setStatus(OrderStatus.PREPARING);
                order.setStartedAt(LocalDateTime.now());

                // Update barista status
                barista.setCurrentOrderId(Math.toIntExact(order.getId()));
                barista.setBusyUntil(LocalDateTime.now().plusMinutes(order.getTotalPrepTime()));
                barista.setTotalWorkTime(barista.getTotalWorkTime() + order.getTotalPrepTime());
                baristaRepository.save(barista);

                order.setPriorityReason("☕ Now brewing with " + barista.getName());
            } else {
                System.out.println("[DEBUG] Barista " + barista.getName() + " is busy - order " + order.getId()
                        + " stays in WAITING");
            }

            Order savedOrder = orderRepository.save(order);
            System.out.println("[DEBUG] Order saved with status: " + savedOrder.getStatus() + ", assignedToBarista: "
                    + savedOrder.getAssignedToBarista());
        } else {
            System.out.println("[DEBUG] WARNING: No barista found for order " + order.getId());
        }
    }

    private int calculateEstimatedWaitTime(Order order, Barista barista, List<Order> waitingOrders) {
        int baseWait = 0;

        // Add barista's current busy time
        if (barista.isBusy()) {
            baseWait = barista.getEstimatedFreeTime();
        }

        // Add time for orders ahead in queue assigned to same barista
        for (Order waiting : waitingOrders) {
            if (waiting.getAssignedToBarista() != null &&
                    waiting.getAssignedToBarista().equals(barista.getId()) &&
                    waiting.getPriorityScore() != null &&
                    order.getPriorityScore() != null &&
                    waiting.getPriorityScore() > order.getPriorityScore()) {
                baseWait += waiting.getTotalPrepTime() * 60; // Convert to seconds
            }
        }

        // Add own prep time
        baseWait += order.getTotalPrepTime() * 60;

        return baseWait;
    }

    public Order startOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        System.out.println("[DEBUG] startOrder called for orderId: " + orderId);

        if (order != null) {
            System.out.println("[DEBUG] Order found. Current status: " + order.getStatus());
            System.out.println("[DEBUG] Order assigned to barista: " + order.getAssignedToBarista());

            if (order.getStatus() == OrderStatus.WAITING) {
                order.setStatus(OrderStatus.PREPARING);
                order.setStartedAt(LocalDateTime.now());
                System.out.println("[DEBUG] Status changed to PREPARING");

                Barista barista = baristaRepository.findById(order.getAssignedToBarista()).orElse(null);
                if (barista != null) {
                    barista.setCurrentOrderId(Math.toIntExact(orderId));
                    barista.setBusyUntil(LocalDateTime.now().plusMinutes(order.getTotalPrepTime()));
                    barista.setTotalWorkTime(barista.getTotalWorkTime() + order.getTotalPrepTime());
                    baristaRepository.save(barista);
                    System.out.println("[DEBUG] Barista " + barista.getName() + " updated");
                } else {
                    System.out.println("[DEBUG] WARNING: No barista found for ID: " + order.getAssignedToBarista());
                }

                Order savedOrder = orderRepository.save(order);
                System.out.println("[DEBUG] Order saved with status: " + savedOrder.getStatus());
                broadcastQueueUpdate();
                return savedOrder;
            } else {
                System.out.println("[DEBUG] Order status is not WAITING, current status: " + order.getStatus());
            }
        } else {
            System.out.println("[DEBUG] Order not found for ID: " + orderId);
        }
        return order;
    }

    public Order completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        System.out.println("[DEBUG] completeOrder called for orderId: " + orderId);

        if (order != null) {
            System.out.println("[DEBUG] Order found. Current status: " + order.getStatus());

            if (order.getStatus() == OrderStatus.PREPARING) {
                order.setStatus(OrderStatus.READY);
                order.setCompletedAt(LocalDateTime.now());
                System.out.println("[DEBUG] Status changed to READY");

                Barista barista = baristaRepository.findById(order.getAssignedToBarista()).orElse(null);
                if (barista != null) {
                    barista.setCurrentOrderId(null);
                    barista.setCompletedOrders(barista.getCompletedOrders() + 1);
                    baristaRepository.save(barista);
                    System.out.println("[DEBUG] Barista " + barista.getName() + " updated");
                } else {
                    System.out.println("[DEBUG] WARNING: No barista found for ID: " + order.getAssignedToBarista());
                }

                Order savedOrder = orderRepository.save(order);
                System.out.println("[DEBUG] Order saved with status: " + savedOrder.getStatus());

                // Trigger re-assignment for other orders
                rebalanceQueue();
                broadcastQueueUpdate();
                return savedOrder;
            } else {
                System.out.println("[DEBUG] Order status is not PREPARING, current status: " + order.getStatus());
            }
        } else {
            System.out.println("[DEBUG] Order not found for ID: " + orderId);
        }
        return order;
    }

    // Recalculate priorities and re-assign every 30 seconds
    @Scheduled(fixedRate = RECALCULATION_INTERVAL)
    public void rebalanceQueue() {
        // First, check and auto-complete any orders that are done brewing
        autoCompleteFinishedOrders();

        // Then, try to assign waiting orders to available baristas
        assignWaitingOrdersToAvailableBaristas();

        List<Order> waitingOrders = getWaitingOrders();
        List<Order> completedOrders = getCompletedOrders();
        List<Barista> baristas = baristaRepository.findAll();

        if (waitingOrders.isEmpty() || baristas.isEmpty()) {
            return;
        }

        // Update workload ratios
        AssignmentAlgorithm.updateWorkloadRatios(baristas);

        // Reassign orders
        AssignmentAlgorithm.assignOrdersToBaristas(waitingOrders, baristas, completedOrders);

        // Update display positions
        List<Order> rankedOrders = AssignmentAlgorithm.rankOrdersByPriority(waitingOrders, completedOrders);
        for (int i = 0; i < rankedOrders.size(); i++) {
            rankedOrders.get(i).setDisplayPosition(i + 1);
            orderRepository.save(rankedOrders.get(i));
        }

        // Check for emergency situations
        checkEmergencies(waitingOrders);

        broadcastQueueUpdate();
    }

    // Auto-complete orders when brewing time is finished
    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void autoCompleteFinishedOrders() {
        List<Order> preparingOrders = getOrdersByStatus(OrderStatus.PREPARING);

        for (Order order : preparingOrders) {
            Barista barista = baristaRepository.findById(order.getAssignedToBarista()).orElse(null);
            if (barista != null && barista.getBusyUntil() != null) {
                // Check if brewing time is over
                if (LocalDateTime.now().isAfter(barista.getBusyUntil())) {
                    System.out
                            .println("[AUTO-COMPLETE] Order " + order.getId() + " brewing complete! Moving to READY.");

                    order.setStatus(OrderStatus.READY);
                    order.setCompletedAt(LocalDateTime.now());
                    orderRepository.save(order);

                    // Free up the barista
                    barista.setCurrentOrderId(null);
                    barista.setBusyUntil(null);
                    barista.setCompletedOrders(barista.getCompletedOrders() + 1);
                    baristaRepository.save(barista);

                    System.out.println("[AUTO-COMPLETE] Barista " + barista.getName() + " is now available.");

                    // Immediately try to assign any waiting orders to this now-available barista
                    assignWaitingOrdersToAvailableBaristas();

                    // Force update workload ratios since an order was completed
                    AssignmentAlgorithm.updateWorkloadRatios(baristaRepository.findAll());

                    broadcastQueueUpdate();
                }
            }
        }
    }

    // Assign waiting orders to available baristas
    private void assignWaitingOrdersToAvailableBaristas() {
        List<Order> waitingOrders = getWaitingOrders();
        List<Barista> baristas = baristaRepository.findAll();

        // Get available baristas
        List<Barista> availableBaristas = baristas.stream()
                .filter(b -> !b.isBusy())
                .collect(java.util.stream.Collectors.toList());

        if (waitingOrders.isEmpty() || availableBaristas.isEmpty()) {
            return;
        }

        // Sort waiting orders by priority (highest first)
        List<Order> completedOrders = getCompletedOrders();
        waitingOrders.sort((o1, o2) -> {
            int score1 = PriorityScoreCalculator.calculatePriorityScore(o1, waitingOrders, completedOrders);
            int score2 = PriorityScoreCalculator.calculatePriorityScore(o2, waitingOrders, completedOrders);
            return score2 - score1;
        });

        // Assign orders to available baristas
        for (Order order : waitingOrders) {
            if (availableBaristas.isEmpty())
                break;

            Barista barista = AssignmentAlgorithm.findBestBarista(order, baristas);
            if (barista != null && !barista.isBusy()) {
                System.out.println("[AUTO-ASSIGN] Assigning waiting order " + order.getId() + " to available barista "
                        + barista.getName());

                order.setAssignedToBarista(barista.getId());
                order.setStatus(OrderStatus.PREPARING);
                order.setStartedAt(LocalDateTime.now());
                order.setPriorityReason("☕ Now brewing with " + barista.getName());
                orderRepository.save(order);

                barista.setCurrentOrderId(Math.toIntExact(order.getId()));
                barista.setBusyUntil(LocalDateTime.now().plusMinutes(order.getTotalPrepTime()));
                barista.setTotalWorkTime(barista.getTotalWorkTime() + order.getTotalPrepTime());
                baristaRepository.save(barista);

                // Remove from available list
                availableBaristas.remove(barista);

                broadcastQueueUpdate();
            }
        }
    }

    private void checkEmergencies(List<Order> waitingOrders) {
        for (Order order : waitingOrders) {
            long secondsWaiting = ChronoUnit.SECONDS.between(order.getCreatedAt(), LocalDateTime.now());

            // Check for 10+ minute wait - generate complaint to manager
            if (secondsWaiting >= 600 && (order.getComplaintGenerated() == null || !order.getComplaintGenerated())) {
                order.setComplaintGenerated(true);
                orderRepository.save(order);
                System.out.println("🚨 COMPLAINT FILED: Order " + order.getId() + " (" + order.getCustomerName()
                        + ") waited 10+ minutes! Manager notified.");
            }

            if (PriorityScoreCalculator.checkTimeoutRisk(order)) {
                // Alert that customer approaching timeout
                System.out.println("ALERT: Order " + order.getId() + " approaching timeout!");
            }

            if (PriorityScoreCalculator.checkAbandonmentRisk(order, order.getIsRegularCustomer())) {
                // Customer likely to abandon
                System.out.println("ALERT: Order " + order.getId() + " at abandonment risk!");
            }
        }
    }

    public void broadcastQueueUpdate() {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("waitingOrders", getActiveOrders());
            update.put("baristas", baristaRepository.findAll());
            messagingTemplate.convertAndSend("/topic/queue-update", update);
        } catch (Exception e) {
            System.err.println("Error broadcasting queue update: " + e.getMessage());
        }
    }

    // Active orders = both WAITING and PREPARING (for queue management)
    private List<Order> getActiveOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.WAITING || o.getStatus() == OrderStatus.PREPARING)
                .collect(Collectors.toList());
    }

    // Waiting orders = only WAITING status (for statistics)
    private List<Order> getWaitingOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.WAITING)
                .collect(Collectors.toList());
    }

    private List<Order> getCompletedOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public Barista getBarista(Integer id) {
        return baristaRepository.findById(id).orElse(null);
    }

    public List<Barista> getAllBaristas() {
        return baristaRepository.findAll();
    }

    private void initializeBaristas() {
        List<Barista> baristas = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Barista barista = new Barista();
            barista.setName("Barista " + i);
            barista.setTotalWorkTime(0);
            barista.setCompletedOrders(0);
            barista.setWorkloadRatio(0.0);
            baristas.add(barista);
        }
        baristaRepository.saveAll(baristas);
    }

    public Map<String, Object> getSystemStats() {
        List<Order> allOrders = orderRepository.findAll();
        List<Order> completedOrders = getCompletedOrders();
        List<Order> waitingOrders = getOrdersByStatus(OrderStatus.WAITING);
        List<Order> preparingOrders = getOrdersByStatus(OrderStatus.PREPARING);

        double avgWaitTime = completedOrders.stream()
                .mapToLong(o -> ChronoUnit.SECONDS.between(o.getCreatedAt(), o.getCompletedAt()))
                .average()
                .orElse(0);

        long timeoutCount = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.ABANDONED || PriorityScoreCalculator.checkTimeoutRisk(o))
                .count();

        List<Barista> baristas = getAllBaristas();
        double workloadStdDev = calculateWorkloadStdDev(baristas);

        // Count complaints filed (orders that waited 10+ minutes)
        long complaintCount = allOrders.stream()
                .filter(o -> o.getComplaintGenerated() != null && o.getComplaintGenerated())
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", allOrders.size());
        stats.put("completedOrders", allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.READY).count());
        stats.put("waitingOrders", waitingOrders.size());
        stats.put("preparingOrders", preparingOrders.size());
        stats.put("readyOrders", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.READY).count());
        stats.put("avgWaitTimeSeconds", (int) avgWaitTime);
        stats.put("timeoutCount", timeoutCount);
        stats.put("timeoutRate", allOrders.isEmpty() ? 0 : (timeoutCount * 100.0 / allOrders.size()));
        stats.put("workloadBalance", 100 - workloadStdDev);
        stats.put("complaintCount", complaintCount);

        return stats;
    }

    private double calculateWorkloadStdDev(List<Barista> baristas) {
        if (baristas.isEmpty())
            return 0;

        double avgWorkload = baristas.stream()
                .mapToInt(Barista::getTotalWorkTime)
                .average()
                .orElse(0);

        double variance = baristas.stream()
                .mapToDouble(b -> Math.pow(b.getTotalWorkTime() - avgWorkload, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance) / (avgWorkload > 0 ? avgWorkload : 1);
    }
}
