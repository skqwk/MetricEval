package ru.skqwk.scheduler.sandbox.util;

import lombok.RequiredArgsConstructor;
import ru.skqwk.scheduler.sandbox.props.Props;
import ru.skqwk.scheduler.sandbox.task.Task;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static java.util.function.Predicate.not;

@RequiredArgsConstructor
public abstract class MetricLogger extends CVLogger {
    protected final Props props;

    private double prevQ = 0;
    private double trend = 0;

    public Double calculateLoad() {
        return lengthWithOverloadNormalized();
    }

    public Double lengthWithOverloadNormalized() {
        long Q = getQueueLength();

        updateTrend(Q);

        double baseLoad = (double) Q / (Q + 1);

        double mu = props.getGlobalAllowedRPM();
        double trendNorm = trend / mu;

        double overload = Math.max(trendNorm, 0);

        return baseLoad + overload;
    }

    public void updateTrend(long currentQ) {
        double instant = currentQ - prevQ;
        double alphaTrend = 0.2;
        trend = alphaTrend * instant + (1 - alphaTrend) * trend;
        prevQ = currentQ;
    }

    private long getQueueLength() {
        return getTasks().stream()
                .filter(Predicate.not(Task::isFinished))
                .count();
    }
}
