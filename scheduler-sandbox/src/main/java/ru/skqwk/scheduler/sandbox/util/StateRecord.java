package ru.skqwk.scheduler.sandbox.util;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
public class StateRecord {
    private final String timestamp;
    private final LocalDateTime dateTime;
    private final int tick;
    private final Object cv;
    private final Object load;
    private final Object strategy;

    /**
     * Конвертирует запись в CSV строку
     */
    public String toCsvString() {
        return String.format("%s,%d,%s,%s,%s", timestamp, tick, cv, load, strategy);
    }

    /**
     * Возвращает заголовок CSV
     */
    public static String getCsvHeader() {
        return "timestamp,tick,cv,load,strategy";
    }

}
