package ru.skqwk.scheduler.sandbox.actor.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class StartRqMsg extends Message {
    private final LocalDateTime now;
}
