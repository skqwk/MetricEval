package ru.skqwk.scheduler.sandbox.task.supplier;

import ru.skqwk.scheduler.sandbox.task.Task;

import java.util.function.Supplier;

public interface TaskDeque extends Supplier<Task> {
    void add(Task task);
}
