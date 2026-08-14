package ru.skqwk.scheduler.sandbox.actor.impl;

import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.sandbox.actor.TalkerActor;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.NewTaskMsg;
import ru.skqwk.scheduler.sandbox.actor.message.QueueEmptyMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.task.supplier.TaskDeque;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
public class TaskDequeActor implements TaskDeque, TalkerActor {
    private final Queue<Task> futureTasks;

    private final Deque<Task> availableTasks = new LinkedList<>();

    public TaskDequeActor(List<Task> futureTasks) {
        this.futureTasks = new LinkedList<>(futureTasks);
    }

    @Override
    public Task get() {
        return availableTasks.poll();
    }

    public List<Task> getTasks() {
        return availableTasks.stream().toList();
    }

    @Override
    public List<Message> onMessage(Message input) {
        if (input instanceof TickMessage message) {
            LocalDateTime now = message.getNow();

            if (futureTasks.isEmpty()) {
                return Collections.singletonList(new QueueEmptyMsg());
            }

            List<Message> messages = new ArrayList<>();
            while (!futureTasks.isEmpty() && futureTasks.element().getCreatedAt().isBefore(now)) {
                Task newTask = futureTasks.poll();
                availableTasks.add(newTask);
                log.info("Пришла новая задача - {}", newTask.getId());
                messages.add(new NewTaskMsg(newTask, now));
            }

            return messages;
        }

        return Collections.emptyList();
    }

    @Override
    public void add(Task task) {
        log.info("Задача {} возвращена в очередь", task.getId());
        availableTasks.addFirst(task);
    }
}
