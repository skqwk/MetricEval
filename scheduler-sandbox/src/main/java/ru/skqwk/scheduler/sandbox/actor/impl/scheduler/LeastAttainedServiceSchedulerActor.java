package ru.skqwk.scheduler.sandbox.actor.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.sandbox.actor.TalkerActorWithQueue;
import ru.skqwk.scheduler.sandbox.actor.message.EndMsg;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.NewTaskMsg;
import ru.skqwk.scheduler.sandbox.actor.message.QueueEmptyMsg;
import ru.skqwk.scheduler.sandbox.actor.message.ReadyRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.ShutdownMsg;
import ru.skqwk.scheduler.sandbox.actor.message.StartRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TaskFinishedMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.task.meta.RequestMetadata;
import ru.skqwk.scheduler.sandbox.util.RequestExecutor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Планировщик на основе алгоритма Least Attained Service (LAS).
 * <p>
 * В любой момент времени процессор назначается заданию, которое получило наименьшее количество
 * обслуживания среди всех активных заданий. В качестве меры обслуживания используется количество
 * выполненных запросов.
 * <p>
 * Если несколько задач имеют одинаковое наименьшее количество выполненных запросов, они
 * обслуживаются в режиме разделения процессора (processor sharing), что в данной реализации
 * означает циклический перебор среди таких задач.
 */
@Slf4j
@RequiredArgsConstructor
public class LeastAttainedServiceSchedulerActor implements TalkerActorWithQueue {
    private final RequestExecutor requestExecutor;

    // Активные задачи (те, которые находятся в системе и могут выполняться)
    private final List<Task> activeTasks = new LinkedList<>();

    // Количество выполненных запросов для каждой задачи
    private final Map<String, Integer> completedRequests = new HashMap<>();

    // Время последней попытки выполнения для отслеживания простоев (опционально)
    private LocalDateTime requestMiss;
    private Duration duration = Duration.ZERO;

    @Override
    public List<Message> onMessage(Message input) {
        if (input instanceof ReadyRqMsg message) {
            return handleReadyRq(message);
        } else if (input instanceof TickMessage message) {
            return handleTick(message);
        } else if (input instanceof NewTaskMsg message) {
            return handleNewTask(message);
        } else if (input instanceof EndMsg) {
            log.info("Накопленное время ожидания - {}", duration);
        } else if (input instanceof QueueEmptyMsg) {
            if (activeTasks.isEmpty()) {
                return Collections.singletonList(new ShutdownMsg(duration));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Обработка готовности к выполнению запроса.
     * Выбирает задачу с наименьшим количеством выполненных запросов и пытается выполнить её запрос.
     */
    private List<Message> handleReadyRq(ReadyRqMsg message) {
        if (activeTasks.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Task> candidates = getCandidates();
        LocalDateTime now = message.getNow();
        for (Task task : candidates) {
            if (requestExecutor.tryExecute(task, now)) {
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

        return Collections.emptyList();
    }

    private List<Task> getCandidates() {
        return activeTasks.stream()
                .sorted(Comparator.comparingInt(task -> completedRequests.getOrDefault(task.getId(), 0)))
                .collect(Collectors.toList());
    }

    /**
     * Обработка тика: проверяет завершённые запросы и обновляет статистику.
     */
    private List<Message> handleTick(TickMessage message) {
        LocalDateTime now = message.getNow();
        List<RequestMetadata> completed = requestExecutor.tryFinish(now);

        List<Message> finished = new ArrayList<>();
        for (RequestMetadata request : completed) {
            Task task = request.getTask();
            String taskId = task.getId();

            // Увеличиваем счётчик выполненных запросов
            completedRequests.merge(taskId, 1, Integer::sum);
            log.debug("Задача {} завершила запрос. Всего выполнено: {}",
                    taskId, completedRequests.get(taskId));

            // Если задача полностью выполнена, удаляем её из активных
            if (task.isFinished()) {
                activeTasks.remove(task);
                completedRequests.remove(taskId);
                finished.add(new TaskFinishedMsg(task, now));
                log.info("Задача {} полностью завершена", taskId);
            }
        }

        return finished;
    }

    /**
     * Обработка новой задачи.
     */
    private List<Message> handleNewTask(NewTaskMsg message) {
        Task task = message.getTask();
        String taskId = task.getId();

        log.info("Новая задача {} добавлена в систему", taskId);

        activeTasks.add(task);
        completedRequests.put(taskId, 0); // начальное количество выполненных запросов = 0

        return Collections.emptyList();
    }

    @Override
    public Collection<Task> getTasks() {
        // Возвращаем копию списка активных задач для внешнего наблюдателя
        return new ArrayList<>(activeTasks);
    }
}
