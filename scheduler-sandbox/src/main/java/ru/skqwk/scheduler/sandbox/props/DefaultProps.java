package ru.skqwk.scheduler.sandbox.props;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public class DefaultProps implements Props {
    private final double globalAllowedRPM;
    private final Duration intervalMetricRecord;
    private final int msInOneTick;
}
