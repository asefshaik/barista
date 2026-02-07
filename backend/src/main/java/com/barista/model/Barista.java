package com.barista.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "baristas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Barista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private Integer currentOrderId; // Currently preparing order
    private LocalDateTime busyUntil; // When will be free
    private Integer totalWorkTime; // Total preparation time
    private Integer completedOrders; // Number of orders completed
    private Double workloadRatio; // Current workload / average

    @PrePersist
    protected void onCreate() {
        totalWorkTime = 0;
        completedOrders = 0;
        workloadRatio = 0.0;
    }

    public boolean isBusy() {
        return busyUntil != null && busyUntil.isAfter(LocalDateTime.now());
    }

    public int getEstimatedFreeTime() {
        if (busyUntil == null) {
            return 0;
        }
        long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), busyUntil);
        return (int) Math.max(0, secondsRemaining);
    }
}
