package ru.skqwk.scheduler.sandbox.props;

import java.time.Duration;

/**
 * Конфигурационные параметры
 */
public interface Props {
    long MINUTE = 60_000;
    long MINUTE_SEC = 60;

    /**
     * Разрешенный RPM
     */
    double getGlobalAllowedRPM();

    /**
     * С каким интервалом собирать метрики
     */
    Duration getIntervalMetricRecord();

    /**
     * Сколько миллисекунд в одном такте
     */
    int getMsInOneTick();

    default long getIntervalInMsBetweenRequests() {
        return (long) (MINUTE / getGlobalAllowedRPM());
    }

    default long getIntervalInSecBetweenRequests() {
        return (long) (MINUTE_SEC / getGlobalAllowedRPM());
    }
}
