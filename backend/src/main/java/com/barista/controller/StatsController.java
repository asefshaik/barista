package com.barista.controller;

import com.barista.service.QueueManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "http://localhost:3000")
public class StatsController {

    @Autowired
    private QueueManagementService queueService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = queueService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
}
