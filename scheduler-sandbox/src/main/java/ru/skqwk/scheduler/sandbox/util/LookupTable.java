package ru.skqwk.scheduler.sandbox.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class LookupTable {
    private double loadMin, loadMax;
    private double cvMin, cvMax;

    private final List<Point> points = new ArrayList<>();

    public String getStrategy(double cv, double load, String nowStrategy) {
        double normCv = (cv - cvMin) / (cvMax - cvMin);
        double normLoad = (load - loadMin) / (loadMax - loadMin);

        return points.stream()
                .min(Comparator.comparing(p -> calculateDistSq(normCv, normLoad, p)))
                .map(point -> getStrategy(point, nowStrategy))
                .orElse(nowStrategy);
    }

    /**
     * Получить стратегию
     *
     * @param nearestPoint ближайшая точка
     * @param nowStrategy  текущая стратегия
     * @return стратегия планирования
     */
    private String getStrategy(Point nearestPoint,
                               String nowStrategy) {
        Double limit = nearestPoint.deviations().get(nowStrategy);
        if (limit < 5) {
            return nowStrategy;
        }

        return nearestPoint.deviations().entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(nowStrategy);
    }

    private double calculateDistSq(double normCv,
                                   double normLoad,
                                   Point p) {
        double dx = p.loadNorm - normLoad;
        double dy = p.cvNorm - normCv;

        return dx * dx + dy * dy;
    }

    /**
     * Точка в таблице поиска
     *
     * @param loadNorm
     * @param cvNorm
     * @param deviations
     */
    record Point(double loadNorm,
                 double cvNorm,
                 Map<String, Double> deviations) {
    }
}
