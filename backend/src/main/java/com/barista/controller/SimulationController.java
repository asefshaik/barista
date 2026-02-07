package com.barista.controller;

import com.barista.model.TestCaseResult;
import com.barista.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001",
        "http://127.0.0.1:3000" }, allowCredentials = "true")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    @PostMapping("/run")
    public ResponseEntity<Object> runSimulation() {
        if (simulationService.isRunning()) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "already_running");
            response.put("message", "Simulation is already in progress");
            return ResponseEntity.ok(response);
        }

        List<TestCaseResult> results = simulationService.runAllTestCases();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("testCases", results);
        response.put("summary", calculateSummary(results));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/results")
    public ResponseEntity<Object> getResults() {
        List<TestCaseResult> results = simulationService.getResults();

        Map<String, Object> response = new HashMap<>();
        response.put("testCases", results);
        response.put("running", simulationService.isRunning());

        if (!results.isEmpty()) {
            response.put("summary", calculateSummary(results));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", simulationService.isRunning());
        status.put("completedTests", simulationService.getResults().size());
        return ResponseEntity.ok(status);
    }

    private Map<String, Object> calculateSummary(List<TestCaseResult> results) {
        if (results.isEmpty()) {
            return new HashMap<>();
        }

        int totalOrders = results.stream().mapToInt(TestCaseResult::getTotalOrders).sum();
        double avgWaitTime = results.stream().mapToDouble(TestCaseResult::getAvgWaitTimeSeconds).average().orElse(0);
        int totalComplaints = results.stream().mapToInt(TestCaseResult::getComplaints).sum();
        int totalAbandoned = results.stream().mapToInt(TestCaseResult::getAbandoned).sum();
        int b1Total = results.stream().mapToInt(TestCaseResult::getBarista1Orders).sum();
        int b2Total = results.stream().mapToInt(TestCaseResult::getBarista2Orders).sum();
        int b3Total = results.stream().mapToInt(TestCaseResult::getBarista3Orders).sum();
        double avgTimeoutRate = results.stream().mapToDouble(TestCaseResult::getTimeoutRate).average().orElse(0);
        double avgAbandonRate = results.stream().mapToDouble(TestCaseResult::getAbandonmentRate).average().orElse(0);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTestCases", results.size());
        summary.put("totalOrders", totalOrders);
        summary.put("avgWaitTimeSeconds", avgWaitTime);
        summary.put("totalComplaints", totalComplaints);
        summary.put("totalAbandoned", totalAbandoned);
        summary.put("avgTimeoutRate", avgTimeoutRate);
        summary.put("avgAbandonRate", avgAbandonRate);
        summary.put("barista1Total", b1Total);
        summary.put("barista2Total", b2Total);
        summary.put("barista3Total", b3Total);
        summary.put("workloadBalance", calculateWorkloadBalance(b1Total, b2Total, b3Total));

        return summary;
    }

    private double calculateWorkloadBalance(int b1, int b2, int b3) {
        double avg = (b1 + b2 + b3) / 3.0;
        if (avg == 0)
            return 100.0;

        double variance = (Math.pow(b1 - avg, 2) + Math.pow(b2 - avg, 2) + Math.pow(b3 - avg, 2)) / 3.0;
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = (stdDev / avg) * 100;

        // Return balance score (100 = perfect balance, lower = more imbalanced)
        return Math.max(0, 100 - coefficientOfVariation);
    }
}
