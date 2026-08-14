package ru.skqwk.scheduler.sandbox.task;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DefaultTask implements Task {
    private String id = UUID.randomUUID().toString();
    private LocalDateTime createdAt;
    private int index;
    private List<Integer> plan;

    @Override
    public boolean isFinished() {
        return index >= plan.size();
    }
}
