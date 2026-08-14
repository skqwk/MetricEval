package ru.skqwk.scheduler.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Result {
    public static final String SET = "set";
    public static final String TAKE_TASK = "takeTask";
    public static final String PAUSE = "pause";

    private final Map<Integer, Double> set = new HashMap<>();
    private final List<Integer> pause = new ArrayList<>();
    private Optional<Double> volume = Optional.empty();

    public Result set(int index, double volume) {
        set.put(index, volume);
        return this;
    }

    public Result pause(int index) {
        pause.add(index);
        return this;
    }

    public Result take(double volume) {
        this.volume = Optional.of(volume);
        return this;
    }

    public Result with(Result other) {
        this.set.putAll(other.set);
        this.pause.addAll(other.pause);
        other.volume.ifPresent(this::take);

        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        if (!this.set.isEmpty()) {
            result.put(SET, set);
        }

        if (!this.pause.isEmpty()) {
            result.put(PAUSE, pause);
        }

        this.volume.ifPresent(take -> result.put(TAKE_TASK, take));

        return result;
    }
}
