package com.barista.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    private List<String> drinks; // List of drink type names

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime displayedAt;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Integer totalPrepTime; // in minutes
    private String customerName;
    private Boolean isRegularCustomer;
    private String loyaltyStatus; // GOLD, SILVER, BRONZE, NONE

    private Integer assignedToBarista; // Barista ID
    private Integer estimatedWaitTime; // in seconds
    private Integer priorityScore; // 0-100
    private String priorityReason; // Explanation for priority

    private Integer displayPosition; // Position in customer queue display
    private Integer hoursBeatenBy; // How many later arrivals were served first
    private Boolean complaintGenerated; // True if order waited 10+ min and complaint was filed

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = OrderStatus.WAITING;
        isRegularCustomer = false;
        loyaltyStatus = "NONE";
        displayPosition = 0;
        hoursBeatenBy = 0;
        complaintGenerated = false;
    }
}
