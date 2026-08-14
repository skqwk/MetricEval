package ru.skqwk.scheduler.sandbox.actor;

import ru.skqwk.scheduler.sandbox.task.Task;

import java.util.Collection;

public interface TalkerActorWithQueue extends TalkerActor {
    Collection<Task> getTasks();
}
