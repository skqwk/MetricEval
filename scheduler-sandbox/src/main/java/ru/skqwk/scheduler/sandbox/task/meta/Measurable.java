package ru.skqwk.scheduler.sandbox.task.meta;

public interface Measurable {
    double getExecutionDurationInSeconds();

    double getActualDurationInSeconds();
}
