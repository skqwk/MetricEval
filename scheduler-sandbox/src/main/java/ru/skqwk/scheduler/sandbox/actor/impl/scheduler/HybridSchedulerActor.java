package ru.skqwk.scheduler.sandbox.actor.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.sandbox.actor.TalkerActorWithQueue;
import ru.skqwk.scheduler.sandbox.actor.message.EndMsg;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.NewTaskMsg;
import ru.skqwk.scheduler.sandbox.actor.message.NowUtilMsg;
import ru.skqwk.scheduler.sandbox.actor.message.QueueEmptyMsg;
import ru.skqwk.scheduler.sandbox.actor.message.ReadyRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.ShutdownMsg;
import ru.skqwk.scheduler.sandbox.actor.message.StartRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TaskFinishedMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.props.Props;
import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.task.meta.RequestMetadata;
import ru.skqwk.scheduler.sandbox.util.LookupTable;
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

@Slf4j
@RequiredArgsConstructor
public class HybridSchedulerActor implements TalkerActorWithQueue {
    private static final String FCFS = "FCFS";
    private static final String LAS = "LAS";
    private static final String RR = "RR";

    private final RequestExecutor requestExecutor;
    private final Props props;
    private final LookupTable lookupTable;

    // Активные задачи (те, которые находятся в системе и могут выполняться)
    private final List<Task> activeTasks = new LinkedList<>();

    // Количество выполненных запросов для каждой задачи
    private final Map<String, Integer> completedRequests = new HashMap<>();

    // Время последней попытки выполнения для отслеживания простоев (опционально)
    private LocalDateTime requestMiss;
    private Duration duration = Duration.ZERO;

    private double coefficientOfVariation = 0;
    private double coefficientOfLoad = 0;
    private int index = 0;

    private Map<String, Integer> counters = new HashMap<>();

    private String nowStrategy = FCFS;

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
        } else if (input instanceof NowUtilMsg message) {
            this.coefficientOfVariation = message.getCoefficientOfVariation();
            this.coefficientOfLoad = message.getCoefficientOfLoad();
        }

        return Collections.emptyList();
    }

    /**
     * Обработка готовности к выполнению запроса: выбирает из задач, отсортированных одним из 3-х алгоритмов
     */
    private List<Message> handleReadyRq(ReadyRqMsg message) {
        if (activeTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> candidates = getCandidates();
        LocalDateTime now = message.getNow();
        for (Task task : candidates) {
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

        return Collections.emptyList();
    }

    private List<Task> getCandidates() {
        updateStrategyLookupTable();
        if (FCFS.equals(nowStrategy)) {
            return activeTasks;
        } else if (LAS.equals(nowStrategy)) {
            return activeTasks.stream()
                    .sorted(Comparator.comparingInt(task -> completedRequests.getOrDefault(task.getId(), 0)))
                    .collect(Collectors.toList());
        } else {
            return shift(activeTasks, index % activeTasks.size());
        }
    }

    private void updateStrategyLookupTable() {
        setStrategy(lookupTable.getStrategy(coefficientOfVariation, coefficientOfLoad, nowStrategy));
    }


    private void setStrategy(String strategy) {
        for (String key : counters.keySet()) {
            if (!strategy.equals(key)) {
                counters.put(key, 0);
            }
        }

        int counter = counters.computeIfAbsent(strategy, key -> 0);
        if (counter > props.getAttempts()) {
            this.nowStrategy = strategy;
        }
        counters.put(strategy, counter + 1);
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

    public static <T> List<T> shift(List<T> list, int shift) {
        if (list == null) {
            return null;
        }
        int size = list.size();
        if (size == 0) {
            return new ArrayList<>(); // return empty list
        }
        int effectiveShift = shift % size;
        if (effectiveShift < 0) {
            effectiveShift += size;
        }
        if (effectiveShift == 0) {
            return new ArrayList<>(list); // return a copy
        }
        List<T> result = new ArrayList<>(size);
        // Add tail part: from effectiveShift to end
        result.addAll(list.subList(effectiveShift, size));
        // Add head part: from 0 to effectiveShift-1
        result.addAll(list.subList(0, effectiveShift));
        return result;
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
        return new ArrayList<>(activeTasks);
    }
}
