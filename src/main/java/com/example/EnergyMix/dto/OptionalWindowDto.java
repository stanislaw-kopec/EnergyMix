package com.example.EnergyMix.dto;

import java.time.Instant;

public record OptionalWindowDto(Instant start, Instant end, double averageCleanPercent) {}
