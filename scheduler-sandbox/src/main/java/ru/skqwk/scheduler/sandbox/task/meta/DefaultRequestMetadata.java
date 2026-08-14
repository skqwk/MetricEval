package ru.skqwk.scheduler.sandbox.task.meta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.skqwk.scheduler.sandbox.task.Task;

import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@RequiredArgsConstructor
public class DefaultRequestMetadata implements RequestMetadata {
    private final Task task;
    private final LocalDateTime arrived;
    private LocalDateTime start;
    private LocalDateTime finish;
}
