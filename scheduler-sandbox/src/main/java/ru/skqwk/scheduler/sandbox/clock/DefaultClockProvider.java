package ru.skqwk.scheduler.sandbox.clock;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Setter
@RequiredArgsConstructor
public class DefaultClockProvider implements ClockProvider {
    private final Instant now;
    private final int dimension;
    private int tick;

    public DefaultClockProvider(int dimension) {
        this.now = Instant.now();
        this.dimension = dimension;
    }

    @Override
    public Clock getClock() {
        return Clock.fixed(now.plus(((long) tick * dimension), ChronoUnit.MILLIS), ZoneId.systemDefault());
    }
}
