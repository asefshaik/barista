package com.barista.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private Integer testCaseNumber;
    private Integer totalOrders;
    private Double avgWaitTimeSeconds;
    private Integer barista1Orders;
    private Integer barista2Orders;
    private Integer barista3Orders;
    private Integer complaints; // Orders from OLD customers that waited 10+ min (manager notified)
    private Integer abandoned; // Orders abandoned (new: 8+ min, old: 10+ min)
    private Double timeoutRate; // Percentage of orders that timed out
    private Double abandonmentRate; // Percentage of orders abandoned
    private List<OrderDetail> orders; // Detailed order list for drill-down

    // Constructor without orders list (for summary view)
    public TestCaseResult(Integer testCaseNumber, Integer totalOrders, Double avgWaitTimeSeconds,
            Integer barista1Orders, Integer barista2Orders, Integer barista3Orders,
            Integer complaints, Integer abandoned, Double timeoutRate, Double abandonmentRate) {
        this.testCaseNumber = testCaseNumber;
        this.totalOrders = totalOrders;
        this.avgWaitTimeSeconds = avgWaitTimeSeconds;
        this.barista1Orders = barista1Orders;
        this.barista2Orders = barista2Orders;
        this.barista3Orders = barista3Orders;
        this.complaints = complaints;
        this.abandoned = abandoned;
        this.timeoutRate = timeoutRate;
        this.abandonmentRate = abandonmentRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetail {
        private Integer orderNumber;
        private String customerName;
        private String loyaltyStatus; // NONE, BRONZE, SILVER, GOLD
        private Boolean isRegular; // true = old customer, false = new
        private Integer assignedBarista;
        private Long waitTimeSeconds;
        private String status; // COMPLETED, ABANDONED, COMPLAINT
        private List<String> drinks;
        private String arrivalTime;
    }
}
