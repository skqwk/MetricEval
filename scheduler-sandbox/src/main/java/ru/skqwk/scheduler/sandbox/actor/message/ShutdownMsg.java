package ru.skqwk.scheduler.sandbox.actor.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public class ShutdownMsg extends Message {
    /**
     * Накопленное время ожидания
     */
    private final Duration duration;
}
