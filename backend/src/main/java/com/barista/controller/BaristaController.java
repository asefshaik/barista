package com.barista.controller;

import com.barista.model.Barista;
import com.barista.service.QueueManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/baristas")
@CrossOrigin(origins = "http://localhost:3000")
public class BaristaController {

    @Autowired
    private QueueManagementService queueService;

    @GetMapping
    public ResponseEntity<List<Barista>> getAllBaristas() {
        List<Barista> baristas = queueService.getAllBaristas();
        return ResponseEntity.ok(baristas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Barista> getBarista(@PathVariable Integer id) {
        Barista barista = queueService.getBarista(id);
        return ResponseEntity.ok(barista);
    }
}
