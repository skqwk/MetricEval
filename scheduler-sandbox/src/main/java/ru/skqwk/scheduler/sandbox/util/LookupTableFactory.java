package ru.skqwk.scheduler.sandbox.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LookupTableFactory {

    public static LookupTable load() {
        return load("lookup/lookup_table.json");
    }

    public static LookupTable load(String resourcePath) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = LookupTableFactory.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Lookup table not found: " + resourcePath);
            }
            JsonNode root = mapper.readTree(is);

            LookupTable table = new LookupTable();

            // Мета-информация
            JsonNode meta = root.get("meta");
            JsonNode ranges = meta.get("original_ranges");
            table.setLoadMin(ranges.get("load").get("min").asDouble());
            table.setLoadMax(ranges.get("load").get("max").asDouble());
            table.setCvMin(ranges.get("cv").get("min").asDouble());
            table.setCvMax(ranges.get("cv").get("max").asDouble());

            // Точки
            JsonNode pointsNode = root.get("points");
            for (JsonNode node : pointsNode) {
                JsonNode empNorm = node.get("empirical_norm");
                JsonNode devNode = node.get("deviation");

                Map<String, Double> deviations = new HashMap<>();
                deviations.put("FCFS", devNode.get("FCFS").asDouble());
                deviations.put("LAS", devNode.get("LAS").asDouble());
                deviations.put("RR", devNode.get("RR").asDouble());

                LookupTable.Point point =
                        new LookupTable.Point(
                                empNorm.get("load").asDouble(),
                                empNorm.get("cv").asDouble(),
                                deviations
                        );

                table.getPoints().add(point);
            }

            log.info("LookupTable загружена: {} точек, load=[{}, {}], cv=[{}, {}]",
                    table.getPoints().size(), table.getLoadMin(), table.getLoadMax(),
                    table.getCvMin(), table.getCvMax());
            return table;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lookup table", e);
        }
    }
}
