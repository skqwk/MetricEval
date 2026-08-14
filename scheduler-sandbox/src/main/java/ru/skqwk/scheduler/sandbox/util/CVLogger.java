package ru.skqwk.scheduler.sandbox.util;

import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.task.meta.RequestMetadata;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public abstract class CVLogger {
    public static final String CV_METHOD = "p90-p50-window-200-lb-smooth-div2";

    private final double alpha = 0.3;

    private final Map<String, Double> state = new HashMap<>();

    public Map<String, Double> calculateCV() {
        return Map.of(
                CV_METHOD,
                calculateP90P50ArrivalWindowLookBackWithDiv(CV_METHOD, 200, 2)
        );
    }

    private double getValue(String name) {
        if (!state.containsKey(name)) {
            updateValue(name, 0.0);
        }
        return state.get(name);
    }

    private void updateValue(String name, double value) {
        state.put(name, value);
    }

    public Double calculateCVbyLastN(int n) {
        // Берём последние N заданий
        List<Integer> requestsPerTask = getTasks().stream()
                .filter(Task::isFinished)
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .limit(n)
                .map(Task::getId)
                .map(this::getSize)
                .toList();

        double mean = requestsPerTask.stream().mapToInt(Integer::intValue).average().orElse(1.0);
        double variance = requestsPerTask.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .average().orElse(0.0);

        return Math.sqrt(variance) / mean;
    }

    public double calculateP90P50ArrivalWindowLookBackWithDiv(String name, int limit, int div) {
        List<Task> window = getTasks().stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .limit(limit)
                .toList();

        List<Integer> finished = window.stream()
                .filter(Task::isFinished)
                .map(Task::getId)
                .map(this::getSize)
                .filter(v -> v > 0)
                .sorted()
                .toList();

        double raw;

        double lastValue = getValue(name);
        if (finished.size() < 10) {
            raw = calculateCVbyLastN(limit / div); // нет данных → держим прошлое
        } else {
            double p50 = percentile(finished, 0.50);
            double p90 = percentile(finished, 0.90);

            raw = (p50 == 0) ? lastValue : (p90 / p50);
        }

        // экспоненциальное сглаживание
        lastValue = alpha * raw + (1 - alpha) * lastValue;
        updateValue(name, lastValue);

        return lastValue;
    }

    private double percentile(List<Integer> sortedValues, double p) {
        return percentileLong(sortedValues.stream().mapToLong(i -> i).boxed().collect(Collectors.toList()), p);
    }

    private double percentileLong(List<Long> sortedValues, double p) {
        int n = sortedValues.size();
        if (n == 1) return sortedValues.get(0);

        double index = p * (n - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) return sortedValues.get(lower);

        double weight = index - lower;
        return sortedValues.get(lower) * (1 - weight) + sortedValues.get(upper) * weight;
    }

    protected abstract List<Task> getTasks();

    protected abstract int getSize(String taskId);

    protected abstract Map<String, List<RequestMetadata>> getHistory();
}
