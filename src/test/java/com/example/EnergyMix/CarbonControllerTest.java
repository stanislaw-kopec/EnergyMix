package com.example.EnergyMix;

import com.example.EnergyMix.controller.CarbonController;
import com.example.EnergyMix.dto.DayAggregateDto;
import com.example.EnergyMix.dto.OptionalWindowDto;
import com.example.EnergyMix.service.CarbonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CarbonController.class)
public class CarbonControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CarbonService service;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        CarbonService carbonService() {
            return org.mockito.Mockito.mock(CarbonService.class);
        }
    }

    @Test
    void testGetMix_returnsAggregatedData() throws Exception {
        List<DayAggregateDto> mock = List.of(
                new DayAggregateDto(LocalDate.now(), Map.of("wind", 30.0), 30.0)
        );

        when(service.fetchGeneration(any(), any())).thenReturn(List.of());
        when(service.aggregateByDays(any(), eq(3), any())).thenReturn(mock);

        mvc.perform(get("/api/mix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cleanPercent").value(30.0));
    }

    @Test
    void testGetOptimal_validHours() throws Exception {
        OptionalWindowDto win = new OptionalWindowDto(
                Instant.parse("2025-01-02T00:00:00Z"),
                Instant.parse("2025-01-02T03:00:00Z"),
                55.5
        );

        when(service.fetchGeneration(any(), any())).thenReturn(List.of());
        when(service.findOptimalWindow(any(), eq(3))).thenReturn(Optional.of(win));

        mvc.perform(get("/api/optimal?hours=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageCleanPercent").value(55.5));
    }

    @Test
    void testGetOptimal_invalidHours() throws Exception {
        mvc.perform(get("/api/optimal?hours=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOptimal_notEnoughData() throws Exception {
        when(service.fetchGeneration(any(), any())).thenReturn(List.of());
        when(service.findOptimalWindow(any(), anyInt())).thenReturn(Optional.empty());

        mvc.perform(get("/api/optimal?hours=3"))
                .andExpect(status().is5xxServerError());
    }
}
