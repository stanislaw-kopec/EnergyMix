package com.example.EnergyMix.dto;

import java.time.Instant;
import java.util.Map;

public record IntervalDto(Instant from, Instant to, Map<String, Double> mix) {}
