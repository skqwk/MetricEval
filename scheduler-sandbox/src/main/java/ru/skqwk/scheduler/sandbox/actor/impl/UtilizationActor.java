package ru.skqwk.scheduler.sandbox.actor.impl;

import lombok.RequiredArgsConstructor;
import ru.skqwk.scheduler.sandbox.actor.TalkerActor;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.NowUtilMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.props.Props;
import ru.skqwk.scheduler.sandbox.util.CVLogger;
import ru.skqwk.scheduler.sandbox.util.MetricLogger;
import ru.skqwk.scheduler.sandbox.util.ValueRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

@RequiredArgsConstructor
public class UtilizationActor implements TalkerActor {
    private final Queue<ValueRecord> load;
    private final Map<String, LinkedList<ValueRecord>> records;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private LocalDateTime lastRecorded;
    private int lastRecordedTick;

    private final Props props;
    private final MetricLogger requestExecutor;

    @Override
    public List<Message> onMessage(Message input) {
        if (input instanceof TickMessage tickMessage) {
            return recordSnapshot(tickMessage);
        }

        return Collections.emptyList();
    }

    /**
     * Записывает текущее состояние метрик для последующего усреднения
     */
    private List<Message> recordSnapshot(TickMessage tickMessage) {
        LocalDateTime now = tickMessage.getNow();
        if (isTimeToRecord(now)) {

            // 1. Расчет порядкового индикатора вариации числа запросов в задании
            Map<String, Double> coefficients = requestExecutor.calculateCV();
            String formattedNow = now.format(formatter);
            lastRecordedTick = tickMessage.getTick();

            coefficients.forEach(
                    (key, value) ->
                            records.computeIfAbsent(key, (k) -> new LinkedList<>())
                                    .add(new ValueRecord(formattedNow, now, lastRecordedTick, value))
            );

            // 2. Расчет эмпирической нагрузки
            Double coefficientOfLoad = requestExecutor.calculateLoad();
            load.add(new ValueRecord(formattedNow, now, lastRecordedTick, coefficientOfLoad));
            lastRecorded = now;

            return Collections.singletonList(new NowUtilMsg(
                    Optional.of(coefficients.getOrDefault(CVLogger.CV_METHOD, 0.0)).orElse(0.0),
                    Optional.of(coefficientOfLoad).orElse(0.0))
            );
        }
        return Collections.emptyList();
    }

    private boolean isTimeToRecord(LocalDateTime now) {
        return lastRecorded == null || Duration.between(lastRecorded, now)
                .minus(props.getIntervalMetricRecord()).toMillis() >= 0;
    }
}