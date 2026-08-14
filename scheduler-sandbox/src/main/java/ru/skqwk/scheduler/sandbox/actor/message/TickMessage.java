package ru.skqwk.scheduler.sandbox.actor.message;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TickMessage extends Message {
    private int tick;
    private LocalDateTime now;
}
