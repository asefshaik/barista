package com.barista.controller;

import com.barista.model.*;
import com.barista.service.QueueManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = { "http://localhost:3000", "http://127.0.0.1:3000" }, allowCredentials = "true")
public class OrderController {

    @Autowired
    private QueueManagementService queueService;

    @PostMapping("/create")
    public ResponseEntity<Object> createOrder(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> drinks = (List<String>) request.get("drinks");
            String customerName = (String) request.get("customerName");
            Boolean isRegular = (Boolean) request.getOrDefault("isRegular", false);
            String loyaltyStatus = (String) request.get("loyaltyStatus");

            if (drinks == null || drinks.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Drinks list cannot be empty");
                return ResponseEntity.badRequest().body(error);
            }

            if (customerName == null || customerName.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Customer name is required");
                return ResponseEntity.badRequest().body(error);
            }

            Order order = queueService.createOrder(drinks, customerName, isRegular, loyaltyStatus);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<Order>> getWaitingOrders() {
        List<Order> orders = queueService.getOrdersByStatus(OrderStatus.WAITING);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/preparing")
    public ResponseEntity<List<Order>> getPreparingOrders() {
        List<Order> orders = queueService.getOrdersByStatus(OrderStatus.PREPARING);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Order>> getCompletedOrders() {
        List<Order> orders = queueService.getOrdersByStatus(OrderStatus.COMPLETED);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/ready")
    public ResponseEntity<List<Order>> getReadyOrders() {
        List<Order> orders = queueService.getOrdersByStatus(OrderStatus.READY);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/start")
    public ResponseEntity<?> startOrder(@PathVariable Long orderId) {
        try {
            Order order = queueService.startOrder(orderId);
            if (order == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Order not found");
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to start order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{orderId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId) {
        try {
            Order order = queueService.completeOrder(orderId);
            if (order == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Order not found");
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to complete order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        Order order = queueService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }
}
