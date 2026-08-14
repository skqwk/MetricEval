package ru.skqwk.scheduler.sandbox.actor.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NowUtilMsg extends Message {
    private final double coefficientOfVariation;
    private final double coefficientOfLoad;
}
