package ru.skqwk.scheduler.sandbox.actor.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.sandbox.actor.TalkerActorWithQueue;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.TaskFinishedMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.util.RequestExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseScheduler implements TalkerActorWithQueue {
    protected final RequestExecutor requestExecutor;

    public List<Message> handleTickMessage(TickMessage message, List<Task> tasks) {
        // 2. Проверяем завершенность задач
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        requestExecutor.tryFinish(message.getNow());

        List<Message> finished = new ArrayList<>();
        tasks.removeIf(task -> {
            if (task.isFinished()) {
                log.info("Задача {} завершена", task.getId());
                finished.add(new TaskFinishedMsg(task, message.getNow()));
                return true;
            }
            return false;
        });
        return finished;
    }
}
