package ru.skqwk.scheduler.algo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Algos {
    LAS("Least Attained Service"),
    FCFS("First Come First Served"),
    RR("Round Robin");

    private final String name;
}
