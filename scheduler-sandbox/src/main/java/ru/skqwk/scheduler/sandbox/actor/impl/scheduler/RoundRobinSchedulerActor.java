package ru.skqwk.scheduler.sandbox.actor.impl.scheduler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.sandbox.actor.message.EndMsg;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.NewTaskMsg;
import ru.skqwk.scheduler.sandbox.actor.message.QueueEmptyMsg;
import ru.skqwk.scheduler.sandbox.actor.message.ReadyRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.ShutdownMsg;
import ru.skqwk.scheduler.sandbox.actor.message.StartRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.util.RequestExecutor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Round Robin
 * <br>
 * Передает выполнение по кругу, сохраняя индекс
 */
@Slf4j
public class RoundRobinSchedulerActor extends BaseScheduler {
    @Getter
    private final List<Task> tasks = new LinkedList<>();

    private LocalDateTime requestMiss;
    private Duration duration = Duration.ZERO;

    private int index = 0;

    public RoundRobinSchedulerActor(RequestExecutor requestExecutor) {
        super(requestExecutor);
    }

    @Override
    public List<Message> onMessage(Message input) {
        if (input instanceof ReadyRqMsg message) {
            // 1. Реагируем на возможность выполнения запроса
            if (tasks.isEmpty()) {
                return Collections.emptyList();
            }

            LocalDateTime now = message.getNow();
            int size = tasks.size();
            for (int i = 0; i < size; i++) {
                Task task = tasks.get((i + index) % size);
                if (requestExecutor.tryExecute(task, now)) {
                    index += 1;
                    if (requestMiss != null) {
                        duration = duration.plus(Duration.between(requestMiss, now));
                        requestMiss = null;
                    }

                    return Collections.singletonList(new StartRqMsg(now));
                }
            }

            if (requestMiss == null) {
                requestMiss = now;
            }
        } else if (input instanceof TickMessage message) {
            return handleTickMessage(message, tasks);
        } else if (input instanceof NewTaskMsg message) {
            // 3. Получаем новые задачи
            log.info("Новая задача {} добавлена в очередь", message.getTask().getId());
            tasks.add(message.getTask());
        } else if (input instanceof EndMsg) {
            log.info("Накопленное время ожидания - {}", duration);
        } else if (input instanceof QueueEmptyMsg) {
            if (tasks.isEmpty()) {
                return Collections.singletonList(new ShutdownMsg(duration));
            }
        }

        return Collections.emptyList();
    }
}
