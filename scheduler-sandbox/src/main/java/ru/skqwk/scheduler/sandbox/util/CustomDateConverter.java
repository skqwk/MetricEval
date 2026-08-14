package ru.skqwk.scheduler.sandbox.util;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.RequiredArgsConstructor;
import ru.skqwk.scheduler.sandbox.clock.ClockProvider;

import java.time.Clock;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
public class CustomDateConverter extends ClassicConverter {
    private static ClockProvider clockProvider;

    // Статический метод для установки провайдера при старте
    public static void setClockProvider(ClockProvider provider) {
        clockProvider = provider;
    }

    @Override
    public String convert(ILoggingEvent event) {
        Clock clock = clockProvider.getClock();

        return DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(clock.getZone())
                .format(clock.instant());
    }
}