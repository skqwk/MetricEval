package ru.skqwk.scheduler.sandbox.actor.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ReadyRqMsg extends Message {
    private final LocalDateTime now;
}
