package ru.skqwk.scheduler.sandbox.task;

import java.time.LocalDateTime;

public interface Task {
    String getId();

    LocalDateTime getCreatedAt();

    boolean isFinished();
}
