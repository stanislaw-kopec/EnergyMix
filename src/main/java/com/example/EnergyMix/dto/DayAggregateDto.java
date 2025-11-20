package com.example.EnergyMix.dto;

import java.time.LocalDate;
import java.util.Map;

public record DayAggregateDto(LocalDate date, Map<String, Double> averagePercents, double cleanPercent) {}
