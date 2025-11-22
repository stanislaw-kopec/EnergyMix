package com.example.EnergyMix.service;

import com.example.EnergyMix.dto.DayAggregateDto;
import com.example.EnergyMix.dto.IntervalDto;
import com.example.EnergyMix.dto.OptionalWindowDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CarbonService {
    private final RestTemplate restTemplate;
    private final String baseUrl = "https://api.carbonintensity.org.uk";
    private static final Set<String> CLEAN = Set.of("biomass","nuclear","hydro","wind","solar");

    public CarbonService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<IntervalDto> fetchGeneration(Instant from, Instant to) {
        String fromStr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'")
                .withZone(ZoneOffset.UTC)
                .format(from);
        String toStr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'")
                .withZone(ZoneOffset.UTC)
                .format(to);

        String url = baseUrl + "/generation/" + fromStr + "/" + toStr;
        ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode root = resp.getBody();

        if (root == null || !root.has("data")) return Collections.emptyList();

        List<IntervalDto> intervals = new ArrayList<>();
        for (JsonNode item : root.get("data")) {
            String fromS = item.get("from").asText();
            String toS = item.get("to").asText();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX").withZone(ZoneOffset.UTC);

            Instant f = Instant.from(fmt.parse(fromS));
            Instant t = Instant.from(fmt.parse(toS));

            Map<String, Double> mix = new HashMap<>();
            JsonNode mixNode = item.get("generationmix");
            if (mixNode != null && mixNode.isArray()) {
                for (JsonNode m : mixNode) {
                    String fuel = m.get("fuel").asText();
                    double perc = m.get("perc").asDouble(0.0);
                    mix.put(fuel.toLowerCase(Locale.ROOT), perc);
                }
            }
            intervals.add(new IntervalDto(f, t, mix));
        }

        intervals.sort(Comparator.comparing(IntervalDto::from));
        return intervals;
    }

    public List<DayAggregateDto> aggregateByDays(List<IntervalDto> intervals, int daysCount, LocalDate startDateUtc) {
        Map<LocalDate, List<IntervalDto>> byDay = new TreeMap<>();
        for (int i = 0; i < daysCount; i++) {
            byDay.put(startDateUtc.plusDays(i), new ArrayList<>());
        }
        for (IntervalDto it : intervals) {
            LocalDate d = it.from().atZone(ZoneOffset.UTC).toLocalDate();
            if(byDay.containsKey(d)) byDay.get(d).add(it);
        }

        List<DayAggregateDto> res = new ArrayList<>();
        for (Map.Entry<LocalDate, List<IntervalDto>> e : byDay.entrySet()) {
            LocalDate day = e.getKey();
            List<IntervalDto> list = e.getValue();
            Map<String, Double> avg = new HashMap<>();

            if (list.isEmpty()) {
                res.add(new DayAggregateDto(day, avg, 0.0));
                continue;
            }

            Set<String> fuels = list.stream()
                    .flatMap(i -> i.mix().keySet().stream())
                    .collect(Collectors.toSet());

            for (String fuel : fuels) {
                double sum = 0.0;
                int count = 0;
                for (IntervalDto itv : list) {
                    Double v = itv.mix().get(fuel);
                    if (v != null) {
                        sum += v;
                        count++;
                    }
                }
                avg.put(fuel, count > 0 ? sum / count : 0.0);
            }

            double cleanPercent = CLEAN.stream()
                    .mapToDouble(f -> avg.getOrDefault(f, 0.0))
                    .sum();

            res.add(new DayAggregateDto(day, avg, cleanPercent));
        }
        return res;
    }

    public Optional<OptionalWindowDto> findOptimalWindow(List<IntervalDto> intervals, int windowHours) {
        if(windowHours < 1 || windowHours > 6) return Optional.empty();
        int windowSize = windowHours * 2;

        if (intervals.size() < windowSize) return Optional.empty();

        double bestAvg = -1.0;
        int bestStartIndex = -1;

        double currentSum = 0;

        double[] cleanPerInterval = new double[intervals.size()];
        for (int i = 0; i < intervals.size(); i++) {
            double s = 0;
            for (String c : CLEAN) {
                s += intervals.get(i).mix().getOrDefault(c, 0.0);
            }
            cleanPerInterval[i] = s;
        }

        for (int i = 0; i < windowSize; i++) currentSum += cleanPerInterval[i];
        bestAvg = currentSum / windowSize;
        bestStartIndex = 0;


        for (int i = windowSize; i < cleanPerInterval.length; i++) {
            currentSum += cleanPerInterval[i];
            currentSum -= cleanPerInterval[i - windowSize];
            double avg = currentSum / windowSize;
            if (avg > bestAvg) {
                bestAvg = avg;
                bestStartIndex = i - windowSize + 1;
            }
        }

        IntervalDto startIt = intervals.get(bestStartIndex);
        IntervalDto endIt = intervals.get(bestStartIndex + windowSize - 1);
        Instant start = startIt.from();
        Instant end = endIt.to();

        return Optional.of(new OptionalWindowDto(start, end, Math.round(bestAvg * 100.0) / 100.0));
    }

}
