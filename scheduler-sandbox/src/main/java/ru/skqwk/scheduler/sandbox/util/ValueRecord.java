package ru.skqwk.scheduler.sandbox.util;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
public class ValueRecord {
    private final String timestamp;
    private final LocalDateTime dateTime;
    private final int tick;
    private final Object value;

    /**
     * Конвертирует запись в CSV строку
     */
    public String toCsvString() {
        return String.format("%s,%d,%s", timestamp, tick, value);
    }

    /**
     * Возвращает заголовок CSV
     */
    public static String getCsvHeader() {
        return "timestamp,tick,value";
    }

}
