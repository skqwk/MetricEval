package ru.skqwk.scheduler.sandbox.actor.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.skqwk.scheduler.sandbox.task.Task;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class NewTaskMsg extends Message {
    private final Task task;
    private final LocalDateTime now;
}
