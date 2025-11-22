package com.example.EnergyMix;

import com.example.EnergyMix.dto.DayAggregateDto;
import com.example.EnergyMix.dto.IntervalDto;
import com.example.EnergyMix.dto.OptionalWindowDto;
import com.example.EnergyMix.service.CarbonService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CarbonServiceTest {

    CarbonService service = new CarbonService(new RestTemplate());

    @Test
    void testAggregateByDays_correctAveragesAndCleanPrecent() {
        LocalDate start = LocalDate.of(2025, 1, 1);

        List<IntervalDto> intervals = List.of(
                new IntervalDto(
                        Instant.parse("2025-01-01T00:00:00Z"),
                        Instant.parse("2025-01-01T00:30:00Z"),
                        Map.of("nuclear", 40.0, "wind", 20.0, "gas", 10.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-01T00:30:00Z"),
                        Instant.parse("2025-01-01T01:00:00Z"),
                        Map.of("nuclear", 20.0, "wind", 40.0, "gas", 10.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-02T00:00:00Z"),
                        Instant.parse("2025-01-02T00:30:00Z"),
                        Map.of("solar", 10.0, "biomass", 10.0)
                )
        );

        List<DayAggregateDto> out = service.aggregateByDays(intervals, 3, start);

        DayAggregateDto d1 = out.get(0);
        assertEquals(LocalDate.of(2025,1,1), d1.date());
        assertEquals(30.0, d1.averagePercents().get("nuclear"));
        assertEquals(30.0, d1.averagePercents().get("wind"));
        assertEquals(10.0, d1.averagePercents().get("gas"));

        assertEquals(60.0, d1.cleanPercent());

        DayAggregateDto d2 = out.get(1);
        assertEquals(20.0, d2.cleanPercent());

        assertEquals(0.0, out.get(2).cleanPercent());
    }

    @Test
    void testFindOptimalWindow_selectsHighestAverageCleanEnergy() {
        List<IntervalDto> intervals = new ArrayList<>();

        intervals = List.of(
                new IntervalDto(
                        Instant.parse("2025-01-01T00:00:00Z"),
                        Instant.parse("2025-01-01T00:30:00Z"),
                        Map.of("wind", 10.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-01T00:30:00Z"),
                        Instant.parse("2025-01-01T01:00:00Z"),
                        Map.of("wind", 20.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-01T01:00:00Z"),
                        Instant.parse("2025-01-01T01:30:00Z"),
                        Map.of("wind", 30.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-01T01:30:00Z"),
                        Instant.parse("2025-01-01T02:00:00Z"),
                        Map.of("wind", 40.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-01T02:00:00Z"),
                        Instant.parse("2025-01-01T02:30:00Z"),
                        Map.of("wind", 50.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-01T02:30:00Z"),
                        Instant.parse("2025-01-01T03:00:00Z"),
                        Map.of("wind", 60.0)
                ),
                new IntervalDto(
                        Instant.parse("2025-01-01T03:00:00Z"),
                        Instant.parse("2025-01-01T03:30:00Z"),
                        Map.of("wind", 5.0)
                )
        );

        Optional<OptionalWindowDto> result = service.findOptimalWindow(intervals, 3);

        assertTrue(result.isPresent());
        OptionalWindowDto win = result.get();

        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), win.start());
        assertEquals(Instant.parse("2025-01-01T03:00:00Z"), win.end());
        assertEquals(35.0, win.averageCleanPercent()); // (10+20+30+40+50+60)/6 = 35
    }

    @Test
    void testFindOptimalWindow_invalidInput() {
        List<IntervalDto> intervals = List.of();
        assertTrue(service.findOptimalWindow(intervals, 0).isEmpty());
        assertTrue(service.findOptimalWindow(intervals, 7).isEmpty());
    }

}
