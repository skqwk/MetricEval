package ru.skqwk.scheduler.sandbox.util;

import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.sandbox.actor.impl.TaskDequeActor;
import ru.skqwk.scheduler.sandbox.props.Props;
import ru.skqwk.scheduler.sandbox.task.DefaultTask;
import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.task.meta.DefaultRequestMetadata;
import ru.skqwk.scheduler.sandbox.task.meta.RequestMetadata;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RequestExecutor extends MetricLogger {
    private final Map<String, Task> tasks = new HashMap<>();
    private final Map<String, List<RequestMetadata>> history = new HashMap<>();
    private final Map<String, LocalDateTime> expectedFinish = new HashMap<>();
    private final TaskDequeActor taskDequeActor;

    public RequestExecutor(TaskDequeActor taskDequeActor, Props props) {
        super(props);
        this.taskDequeActor = taskDequeActor;
    }

    @Override
    protected int getSize(String taskId) {
        List<RequestMetadata> requests = history.get(taskId);
        return requests == null ? 0 : requests.size();
    }

    public Map<String, List<RequestMetadata>> getHistory() {
        return history;
    }

    public boolean tryExecute(Task task, LocalDateTime now) {
        String taskId = task.getId();
        if (!tasks.containsKey(taskId)) {
            tasks.put(taskId, task);
        }

        List<RequestMetadata> requests = history.computeIfAbsent(taskId, key -> new ArrayList<>());
        if (requests.isEmpty()) {
            requests.add(new DefaultRequestMetadata(task, task.getCreatedAt()));
        }

        if (getLastRequest(requests).getStart() != null) {
            return false;
        }

        getLastRequest(requests).setStart(now);
        updateExpectedFinish(task, now);

        return true;
    }

    private void updateExpectedFinish(Task task, LocalDateTime now) {
        DefaultTask defaultTask = (DefaultTask) task;
        List<Integer> plan = defaultTask.getPlan();
        int durationInSeconds = plan.get(defaultTask.getIndex());
        expectedFinish.put(task.getId(), now.plus(durationInSeconds, ChronoUnit.SECONDS));
    }

    private DefaultRequestMetadata getLastRequest(List<RequestMetadata> requests) {
        return (DefaultRequestMetadata) requests.get(requests.size() - 1);
    }

    public List<RequestMetadata> tryFinish(LocalDateTime now) {
        List<RequestMetadata> finishedRequests = new ArrayList<>();
        expectedFinish.forEach((key, time) -> {
            List<RequestMetadata> requests = history.get(key);
            RequestMetadata lastRequest = getLastRequest(requests);
            if (lastRequest.getFinish() == null && lastRequest.getStart() != null) {
                if (now.isAfter(time) || now.isEqual(time)) {
                    DefaultRequestMetadata metadata = (DefaultRequestMetadata) lastRequest;
                    metadata.setFinish(now);

                    DefaultTask task = (DefaultTask) metadata.getTask();
                    task.setIndex(task.getIndex() + 1);

                    finishedRequests.add(metadata);

                    if (!task.isFinished()) {
                        history.get(key).add(new DefaultRequestMetadata(task, now));
                    }
                }
            }
        });

        return finishedRequests;
    }

    @Override
    protected List<Task> getTasks() {
        return taskDequeActor.getTasks();
    }
}
