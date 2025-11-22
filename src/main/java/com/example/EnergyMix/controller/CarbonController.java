package com.example.EnergyMix.controller;


import com.example.EnergyMix.dto.DayAggregateDto;
import com.example.EnergyMix.dto.IntervalDto;
import com.example.EnergyMix.dto.OptionalWindowDto;
import com.example.EnergyMix.service.CarbonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class CarbonController {

    private final CarbonService service;

    public CarbonController(CarbonService service) {
        this.service = service;
    }

    @GetMapping("/mix")
    public ResponseEntity<List<DayAggregateDto>> getMix() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        Instant from = todayUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = todayUtc.plusDays(2).atTime(LocalTime.of(23,30)).toInstant(ZoneOffset.UTC);

        List<IntervalDto> intervals = service.fetchGeneration(from, to);

        List<DayAggregateDto> aggregated = service.aggregateByDays(intervals, 3, todayUtc);
        return ResponseEntity.ok(aggregated);
    }

    @GetMapping("/optimal")
    public ResponseEntity<?> getOptional(@RequestParam int hours) {

        if (hours < 1 || hours > 24) return ResponseEntity.badRequest().body("Hours must be between 1 and 24");

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        Instant from = todayUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = todayUtc.plusDays(2).atTime(LocalTime.of(23, 30)).toInstant(ZoneOffset.UTC);

        List<IntervalDto> intervals = service.fetchGeneration(from, to);

        Optional<OptionalWindowDto> maybe = service.findOptimalWindow(intervals, hours);
        if (maybe.isEmpty()) return ResponseEntity.status(500).body("Not enough data to compute optimal window");
        return ResponseEntity.ok(maybe.get());
    }

}
