package ru.skqwk.scheduler.sandbox.task.meta;

import ru.skqwk.scheduler.sandbox.task.Task;

import java.time.Duration;
import java.time.LocalDateTime;

public interface RequestMetadata extends Measurable {
    Task getTask();

    default String getTaskId() {
        return getTask().getId();
    }

    LocalDateTime getStart();

    LocalDateTime getArrived();

    LocalDateTime getFinish();

    default Duration getActualDuration() {
        return Duration.between(getArrived(), getFinish());
    }

    @Override
    default double getExecutionDurationInSeconds() {
        return (double) Duration.between(getStart(), getFinish()).toMillis() / 1_000;
    }

    @Override
    default double getActualDurationInSeconds() {
        return (double) getActualDuration().toMillis() / 1_000L;
    }
}
